package pro.dpsn.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.dpsn.warehouse.dto.CreateInvoiceRequest;
import pro.dpsn.warehouse.dto.ImportResult;
import pro.dpsn.warehouse.dto.InvoiceResponse;
import pro.dpsn.warehouse.dto.ReceiveInvoiceRequest;
import pro.dpsn.warehouse.model.*;
import pro.dpsn.warehouse.repo.*;
import pro.dpsn.warehouse.service.InvoiceService;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UnitRepository unitRepository;
    private final MovementRepository movementRepository;
    private final BulkStockRepository bulkStockRepository;
    private final BoxRepository boxRepository;
    private final BulkItemRepository bulkItemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Invoice createInvoice(CreateInvoiceRequest request, String username) {
        Invoice invoice = new Invoice();
        invoice.setCustomer(request.getCustomer());

        // Ищем пользователя по телефону и берем его ФИО
        User creator = userRepository.findByPhone(username).orElse(null);
        String creatorName = (creator != null) ? creator.getFullName() : username;

        invoice.setResponsible(request.getResponsible() != null ? request.getResponsible() : creatorName);
        invoice.setCreatorName(creatorName); // <-- Сохраняем имя в новое поле

        invoice.setStatus("draft");
        invoice.setCreatedAt(ZonedDateTime.now());
        invoice.setUpdatedAt(ZonedDateTime.now());

        for (CreateInvoiceRequest.ItemRequest itemReq : request.getItems()) {
            InvoiceItem item = new InvoiceItem();
            item.setUnitId(itemReq.getUnitId());
            item.setBulkItemId(itemReq.getBulkItemId());
            item.setBoxId(itemReq.getBoxId());
            item.setQtyPlanned(itemReq.getQtyPlanned() != null ? itemReq.getQtyPlanned() : 1);
            item.setQtyShipped(0);
            item.setQtyReturned(0);
            invoice.addItem(item);
        }
        return invoiceRepository.save(invoice);
    }

    @Override
    public InvoiceResponse getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow(() -> new RuntimeException("Накладная не найдена"));
        InvoiceResponse resp = new InvoiceResponse();
        resp.setId(invoice.getId());
        resp.setCustomer(invoice.getCustomer());
        resp.setResponsible(invoice.getResponsible());
        resp.setStatus(invoice.getStatus());
        resp.setCreatedAt(invoice.getCreatedAt());
        resp.setShippedBy(invoice.getShippedBy());
        resp.setCreatorName(invoice.getCreatorName());

        resp.setItems(invoice.getItems().stream().map(item -> {
            InvoiceResponse.ItemDto dto = new InvoiceResponse.ItemDto();
            dto.setId(item.getId());
            dto.setUnitId(item.getUnitId());
            dto.setBulkItemId(item.getBulkItemId());
            dto.setQtyPlanned(item.getQtyPlanned());
            dto.setQtyShipped(item.getQtyShipped());
            dto.setQtyReturned(item.getQtyReturned());
            dto.setBoxId(item.getBoxId());

            if (item.getUnitId() != null) {
                unitRepository.findById(item.getUnitId()).ifPresent(u -> dto.setUnitName(u.getModel().getName() + " (" + u.getSerialNumber() + ")"));
            }
            if (item.getBulkItemId() != null) {
                bulkItemRepository.findById(item.getBulkItemId()).ifPresent(b -> dto.setBulkItemName(b.getName()));
            }
            if (item.getBoxId() != null) {
                boxRepository.findById(item.getBoxId()).ifPresent(b -> dto.setBoxName(b.getName() != null ? b.getName() : b.getCode()));
            }
            return dto;
        }).toList());
        return resp;
    }

    @Override
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceRepository.findAllByOrderByIdDesc().stream().map(i -> getInvoiceById(i.getId())).toList();
    }

    @Override
    @Transactional
    public void shipInvoice(Long invoiceId, String username) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new RuntimeException("Накладная не найдена"));
        if (!"draft".equals(invoice.getStatus())) throw new RuntimeException("Отгрузить можно только черновик");

        User shipper = userRepository.findByPhone(username).orElse(null);
        String shipperName = (shipper != null) ? shipper.getFullName() : username;

        invoice.setStatus("shipped");
        invoice.setShippedBy(shipperName);
        invoice.setUpdatedAt(ZonedDateTime.now());
        invoice.setLockedBy(null);
        invoice.setLockedAt(null);

        // Множество для отслеживания уже обработанных ящиков в этой накладной
        java.util.Set<Long> processedBoxes = new java.util.HashSet<>();

        for (InvoiceItem item : invoice.getItems()) {
            // 1. ЯЩИК
            if (item.getBoxId() != null && !processedBoxes.contains(item.getBoxId())) {
                Box box = boxRepository.findById(item.getBoxId()).orElseThrow(() -> new RuntimeException("Ящик не найден"));

                // Проверяем статус ТОЛЬКО при первом упоминании ящика
                if (!"on_stock".equals(box.getStatus())) {
                    throw new RuntimeException("Ящик " + box.getCode() + " недоступен (статус: " + box.getStatus() + ")");
                }

                // Проверка лимита для сумок
                if (box.getMaxCells() != null && box.getMaxCells() > 0) {
                    long currentCount = unitRepository.countByCurrentBoxId(box.getId());
                    long addingCount = invoice.getItems().stream().filter(i -> box.getId().equals(i.getBoxId()) && i.getUnitId() != null).count();
                    if (currentCount + addingCount > box.getMaxCells()) {
                        throw new RuntimeException("В сумке " + box.getCode() + " недостаточно места (максимум " + box.getMaxCells() + " ячеек)");
                    }
                }

                box.setStatus("in_transit");
                boxRepository.save(box);
                processedBoxes.add(box.getId()); // Запоминаем, что обработали
            }

            // 2. ОБОРУДОВАНИЕ С QR (без изменений)
            if (item.getUnitId() != null) {
                Unit unit = unitRepository.findById(item.getUnitId()).orElseThrow(() -> new RuntimeException("Оборудование не найдено"));
                if (!"warehouse".equals(unit.getStatus())) throw new RuntimeException("Оборудование " + unit.getSerialNumber() + " недоступно");

                String oldStatus = unit.getStatus();
                unit.setStatus("client");
                unit.setUpdatedAt(ZonedDateTime.now());
                if (item.getBoxId() != null) unit.setCurrentBoxId(item.getBoxId());
                unitRepository.save(unit);

                Movement movement = new Movement();
                movement.setUnitId(unit.getId());
                movement.setInvoiceId(invoice.getId());
                movement.setFromStatus(oldStatus);
                movement.setToStatus("client");
                movement.setComment("Отгрузка по накладной №" + invoice.getId());
                movementRepository.save(movement);
            }

            // 3. МЕЛКОЕ (без изменений)
            if (item.getBulkItemId() != null) {
                BulkStock stock = bulkStockRepository.findById(item.getBulkItemId()).orElseThrow(() -> new RuntimeException("Мелкое оборудование не найдено"));
                if (stock.getQtyOnHand() < item.getQtyPlanned()) throw new RuntimeException("Недостаточно на складе: " + stock.getQtyOnHand() + " шт.");

                stock.setQtyOnHand(stock.getQtyOnHand() - item.getQtyPlanned());
                bulkStockRepository.save(stock);

                Movement movement = new Movement();
                movement.setBulkItemId(item.getBulkItemId());
                movement.setInvoiceId(invoice.getId());
                movement.setFromStatus("warehouse");
                movement.setToStatus("client");
                movement.setQty(item.getQtyPlanned());
                movement.setComment("Отгрузка по накладной №" + invoice.getId());
                movementRepository.save(movement);
            }
            item.setQtyShipped(item.getQtyPlanned());
        }
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void receiveInvoice(Long invoiceId, ReceiveInvoiceRequest request, String username) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Накладная не найдена"));

        if (!"shipped".equals(invoice.getStatus()) && !"partially_returned".equals(invoice.getStatus())) {
            throw new RuntimeException("Приёмка возможна только для отгруженных накладных");
        }

        for (InvoiceItem item : invoice.getItems()) {
            // 1. Обработка QR-оборудования
            if (item.getUnitId() != null && request.getUnitIds() != null && request.getUnitIds().contains(item.getUnitId())) {
                if (item.getQtyReturned() > 0) continue; // Уже принято

                Unit unit = unitRepository.findById(item.getUnitId())
                        .orElseThrow(() -> new RuntimeException("Оборудование не найдено"));

                String oldStatus = unit.getStatus();
                unit.setStatus("warehouse");
                unit.setCurrentBoxId(null);
                unit.setUpdatedAt(ZonedDateTime.now());

                // Если не сканировали — ставим флаг
                if (request.getScannedUnitIds() != null && !request.getScannedUnitIds().contains(item.getUnitId())) {
                    unit.setNeedsNewQr(true);
                } else {
                    unit.setNeedsNewQr(false);
                }
                unitRepository.save(unit);

                Movement movement = new Movement();
                movement.setUnitId(unit.getId());
                movement.setInvoiceId(invoice.getId());
                movement.setFromStatus(oldStatus);
                movement.setToStatus("warehouse");
                movement.setComment("Возврат по накладной №" + invoice.getId());
                movementRepository.save(movement);

                item.setQtyReturned(item.getQtyShipped());
            }

            // 2. Обработка мелкого оборудования (bulk)
            if (item.getBulkItemId() != null) {
                // Смотрим, сколько вернули по этому ID
                Integer returnedQty = (request.getBulkReturns() != null)
                        ? request.getBulkReturns().getOrDefault(item.getBulkItemId(), 0)
                        : 0;

                if (returnedQty > 0) {
                    // Проверяем, чтобы не вернули больше, чем отгрузили
                    if (returnedQty > (item.getQtyShipped() - item.getQtyReturned())) {
                        throw new RuntimeException("Нельзя вернуть больше, чем отгружено");
                    }

                    BulkStock stock = bulkStockRepository.findById(item.getBulkItemId())
                            .orElseThrow(() -> new RuntimeException("Мелкое оборудование не найдено"));

                    stock.setQtyOnHand(stock.getQtyOnHand() + returnedQty);
                    bulkStockRepository.save(stock);

                    Movement movement = new Movement();
                    movement.setBulkItemId(item.getBulkItemId());
                    movement.setInvoiceId(invoice.getId());
                    movement.setFromStatus("client");
                    movement.setToStatus("warehouse");
                    movement.setQty(returnedQty);
                    movement.setComment("Возврат по накладной №" + invoice.getId());
                    movementRepository.save(movement);

                    // Увеличиваем счетчик возвращенного
                    item.setQtyReturned(item.getQtyReturned() + returnedQty);
                }
            }
        }

        // Проверяем, всё ли вернули (и QR, и мелкое)
        boolean allReturned = invoice.getItems().stream()
                .allMatch(i -> i.getQtyReturned() >= i.getQtyShipped());

        if (allReturned) {
            invoice.setStatus("returned");
        } else {
            invoice.setStatus("partially_returned");
        }

        invoice.setUpdatedAt(ZonedDateTime.now());
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void removeItemFromInvoice(Long invoiceId, Long itemId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Накладная не найдена"));

        if (!"draft".equals(invoice.getStatus())) {
            throw new RuntimeException("Удалять позиции можно только из черновика");
        }

        boolean removed = invoice.getItems().removeIf(item -> item.getId().equals(itemId));

        if (!removed) {
            throw new RuntimeException("Позиция с ID " + itemId + " не найдена в накладной");
        }

        invoice.setUpdatedAt(ZonedDateTime.now());
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void addItemToInvoice(Long invoiceId, CreateInvoiceRequest.ItemRequest itemReq) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Накладная не найдена"));

        if (!"draft".equals(invoice.getStatus())) {
            throw new RuntimeException("Редактировать можно только черновик");
        }

        InvoiceItem item = new InvoiceItem();
        item.setUnitId(itemReq.getUnitId());
        item.setBulkItemId(itemReq.getBulkItemId());
        item.setBoxId(itemReq.getBoxId());
        item.setQtyPlanned(itemReq.getQtyPlanned() != null ? itemReq.getQtyPlanned() : 1);
        item.setQtyShipped(0);
        item.setQtyReturned(0);

        invoice.addItem(item);
        invoice.setUpdatedAt(ZonedDateTime.now());
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public void updateItemQty(Long invoiceId, Long itemId, Integer qty) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Накладная не найдена"));

        if (!"draft".equals(invoice.getStatus())) {
            throw new RuntimeException("Редактировать можно только черновик");
        }

        InvoiceItem item = invoice.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Позиция не найдена"));

        if (item.getUnitId() != null) {
            throw new RuntimeException("Нельзя изменить количество для серийного оборудования");
        }

        item.setQtyPlanned(qty);
        invoice.setUpdatedAt(ZonedDateTime.now());
        invoiceRepository.save(invoice);
    }

    @Override
    public byte[] exportInvoiceToExcel(Long invoiceId) {
        // Заглушка, чтобы компилятор не ругался. Позже реализуем по-настоящему.
        throw new RuntimeException("Экспорт в Excel пока не реализован");
    }

    @Override
    @Transactional
    public ImportResult importInvoiceFromExcel(org.springframework.web.multipart.MultipartFile file, String username) {
        // Заглушка. Если нужно — реализуем по-настоящему.
        throw new RuntimeException("Импорт из Excel пока не реализован");
    }

    @Override
    @Transactional
    public void deleteDraftInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Накладная не найдена"));
        if (!"draft".equals(invoice.getStatus())) {
            throw new RuntimeException("Удалять можно только черновики");
        }
        invoiceRepository.delete(invoice);
    }

    @Override
    @Transactional
    public void updateItemBox(Long invoiceId, Long itemId, Long boxId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Накладная не найдена"));

        if (!"draft".equals(invoice.getStatus())) {
            throw new RuntimeException("Менять ящики можно только в черновике");
        }

        InvoiceItem item = invoice.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Позиция не найдена"));

        // Если ящик указан, проверяем, что он существует и свободен
        if (boxId != null) {
            Box box = boxRepository.findById(boxId)
                    .orElseThrow(() -> new RuntimeException("Ящик не найден"));
            if (!"on_stock".equals(box.getStatus())) {
                throw new RuntimeException("Ящик " + box.getCode() + " недоступен (статус: " + box.getStatus() + ")");
            }
        }

        item.setBoxId(boxId);
        invoice.setUpdatedAt(ZonedDateTime.now());
        invoiceRepository.save(invoice);
    }

    @Override
    public InvoiceItem scanItemInInvoice(Long invoiceId, String qrCode) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Накладная не найдена"));

        Unit unit = unitRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new RuntimeException("Оборудование с таким QR не найдено"));

        // Ищем этот юнит в накладной
        return invoice.getItems().stream()
                .filter(item -> unit.getId().equals(item.getUnitId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Этого оборудования нет в выбранной накладной"));
    }
}