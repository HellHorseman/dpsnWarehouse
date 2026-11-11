package pro.dpsn.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.dpsn.warehouse.dto.CreateInvoiceRequest;
import pro.dpsn.warehouse.dto.InvoiceResponse;
import pro.dpsn.warehouse.model.*;
import pro.dpsn.warehouse.repo.*;
import pro.dpsn.warehouse.service.InvoiceService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    @Override
    @Transactional
    public Invoice createInvoice(CreateInvoiceRequest request, String username) {
        Invoice invoice = new Invoice();
        invoice.setCustomer(request.getCustomer());
        invoice.setResponsible(request.getResponsible());
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
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Накладная не найдена"));

        InvoiceResponse resp = new InvoiceResponse();
        resp.setId(invoice.getId());
        resp.setCustomer(invoice.getCustomer());
        resp.setResponsible(invoice.getResponsible());
        resp.setStatus(invoice.getStatus());
        resp.setCreatedAt(invoice.getCreatedAt());

        resp.setItems(invoice.getItems().stream().map(item -> {
            InvoiceResponse.ItemDto dto = new InvoiceResponse.ItemDto();
            dto.setId(item.getId());
            dto.setUnitId(item.getUnitId());
            dto.setQtyPlanned(item.getQtyPlanned());
            dto.setQtyShipped(item.getQtyShipped());
            dto.setQtyReturned(item.getQtyReturned()); // Добавили для полноты
            return dto;
        }).toList());

        return resp;
    }

    @Override
    @Transactional
    public void shipInvoice(Long invoiceId, String shippedBy, String username) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Накладная не найдена"));

        if (!"draft".equals(invoice.getStatus())) {
            throw new RuntimeException("Накладную можно отгрузить только из статуса 'draft'");
        }

        invoice.setStatus("shipped");
        invoice.setShippedBy(shippedBy);
        invoice.setUpdatedAt(ZonedDateTime.now());
        invoice.setLockedBy(null);
        invoice.setLockedAt(null);

        for (InvoiceItem item : invoice.getItems()) {
            // 1. Обработка ЯЩИКА (независимо от содержимого)
            if (item.getBoxId() != null) {
                Box box = boxRepository.findById(item.getBoxId())
                        .orElseThrow(() -> new RuntimeException("Ящик не найден"));

                if (!"on_stock".equals(box.getStatus())) {
                    throw new RuntimeException("Ящик " + box.getCode() + " недоступен (статус: " + box.getStatus() + ")");
                }

                // Проверка лимита ячеек для сумок
                if (box.getType() == BoxType.FIXED_BAG || box.getType() == BoxType.FIXED_BAG_OPTIONAL) {
                    long currentCount = unitRepository.countByCurrentBoxId(box.getId());
                    if (currentCount >= box.getMaxCells()) {
                        throw new RuntimeException("Сумка " + box.getCode() + " заполнена (максимум " + box.getMaxCells() + " ячеек)");
                    }
                }

                box.setStatus("in_transit");
                boxRepository.save(box);
            }

            // 2. Обработка ОБОРУДОВАНИЯ С QR (Unit)
            if (item.getUnitId() != null) {
                Unit unit = unitRepository.findById(item.getUnitId())
                        .orElseThrow(() -> new RuntimeException("Оборудование не найдено"));

                if (!"warehouse".equals(unit.getStatus())) {
                    throw new RuntimeException("Оборудование " + unit.getSerialNumber() + " недоступно (статус: " + unit.getStatus() + ")");
                }

                String oldStatus = unit.getStatus();
                unit.setStatus("client");
                unit.setUpdatedAt(ZonedDateTime.now());

                if (item.getBoxId() != null) {
                    unit.setCurrentBoxId(item.getBoxId());
                }
                unitRepository.save(unit);

                Movement movement = new Movement();
                movement.setUnitId(unit.getId());
                movement.setInvoiceId(invoice.getId());
                movement.setFromStatus(oldStatus);
                movement.setToStatus("client");
                movement.setComment("Отгрузка по накладной №" + invoice.getId());
                movementRepository.save(movement);
            }

            // 3. Обработка МЕЛКОГО ОБОРУДОВАНИЯ (BulkItem)
            if (item.getBulkItemId() != null) {
                BulkStock stock = bulkStockRepository.findById(item.getBulkItemId())
                        .orElseThrow(() -> new RuntimeException("Мелкое оборудование не найдено"));

                if (stock.getQtyOnHand() < item.getQtyPlanned()) {
                    throw new RuntimeException("Недостаточно на складе: " + stock.getQtyOnHand() + " шт.");
                }

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
    public void receiveInvoice(Long invoiceId, List<Long> receivedUnitIds, String username) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Накладная не найдена"));

        if (!"shipped".equals(invoice.getStatus()) && !"partially_returned".equals(invoice.getStatus())) {
            throw new RuntimeException("Приёмка возможна только для отгруженных накладных");
        }

        for (InvoiceItem item : invoice.getItems()) {
            // 1. Обработка ОБОРУДОВАНИЯ С QR (Unit)
            if (item.getUnitId() != null) {
                if (receivedUnitIds.contains(item.getUnitId())) {
                    if (item.getQtyReturned() > 0) {
                        continue;
                    }

                    Unit unit = unitRepository.findById(item.getUnitId())
                            .orElseThrow(() -> new RuntimeException("Оборудование не найдено"));

                    String oldStatus = unit.getStatus();
                    unit.setStatus("warehouse");
                    unit.setCurrentBoxId(null);
                    unit.setUpdatedAt(ZonedDateTime.now());
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
            }

            // 2. Обработка МЕЛКОГО ОБОРУДОВАНИЯ (BulkItem)
            if (item.getBulkItemId() != null) {
                int returnQty = item.getQtyShipped() - item.getQtyReturned();
                if (returnQty > 0) {
                    BulkStock stock = bulkStockRepository.findById(item.getBulkItemId())
                            .orElseThrow(() -> new RuntimeException("Мелкое оборудование не найдено"));

                    stock.setQtyOnHand(stock.getQtyOnHand() + returnQty);
                    bulkStockRepository.save(stock);

                    Movement movement = new Movement();
                    movement.setBulkItemId(item.getBulkItemId());
                    movement.setInvoiceId(invoice.getId());
                    movement.setFromStatus("client");
                    movement.setToStatus("warehouse");
                    movement.setQty(returnQty);
                    movement.setComment("Возврат по накладной №" + invoice.getId());
                    movementRepository.save(movement);

                    item.setQtyReturned(item.getQtyShipped());
                }
            }

            // 3. ЯЩИК НЕ ТРОГАЕМ — он принимается отдельно через BoxService.receiveBox()
        }

        // Проверяем, всё ли вернули (только для QR-оборудования)
        boolean allReturned = invoice.getItems().stream()
                .filter(i -> i.getUnitId() != null)
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

        // Удалять можно только из черновика
        if (!"draft".equals(invoice.getStatus())) {
            throw new RuntimeException("Нельзя удалять позиции из отгруженной накладной");
        }

        // Ищем строку и удаляем её
        boolean removed = invoice.getItems().removeIf(item -> item.getId().equals(itemId));

        if (!removed) {
            throw new RuntimeException("Позиция с ID " + itemId + " не найдена в накладной");
        }

        invoice.setUpdatedAt(ZonedDateTime.now());
        invoiceRepository.save(invoice); // orphanRemoval = true удалит строку из БД
    }

    @Override
    public byte[] exportInvoiceToExcel(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Накладная не найдена"));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Накладная " + invoice.getId());

            // Стили для заголовков
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Заголовки
            Row header = sheet.createRow(0);
            String[] columns = {"ID", "Оборудование/Мелкое", "Кол-во план", "Кол-во отгружено", "Кол-во возвращено"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Данные
            int rowNum = 1;
            for (InvoiceItem item : invoice.getItems()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(item.getId());

                String itemName = item.getUnitId() != null ? "Unit ID: " + item.getUnitId() : "Bulk ID: " + item.getBulkItemId();
                row.createCell(1).setCellValue(itemName);
                row.createCell(2).setCellValue(item.getQtyPlanned());
                row.createCell(3).setCellValue(item.getQtyShipped());
                row.createCell(4).setCellValue(item.getQtyReturned());
            }

            // Автоширина
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка генерации Excel: " + e.getMessage());
        }
    }
}