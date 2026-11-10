package pro.dpsn.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.dpsn.warehouse.dto.CreateInvoiceRequest;
import pro.dpsn.warehouse.dto.InvoiceResponse;
import pro.dpsn.warehouse.model.Invoice;
import pro.dpsn.warehouse.model.InvoiceItem;
import pro.dpsn.warehouse.model.Movement;
import pro.dpsn.warehouse.model.Unit;
import pro.dpsn.warehouse.repo.InvoiceRepository;
import pro.dpsn.warehouse.repo.MovementRepository;
import pro.dpsn.warehouse.repo.UnitRepository;
import pro.dpsn.warehouse.service.InvoiceService;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UnitRepository unitRepository;
    private final MovementRepository movementRepository;

    @Override
    @Transactional
    public Invoice createInvoice(CreateInvoiceRequest request, String username) {
        Invoice invoice = new Invoice();
        invoice.setCustomer(request.getCustomer());
        invoice.setResponsible(request.getResponsible());
        invoice.setStatus("draft"); // Накладная создается как черновик
        invoice.setCreatedAt(ZonedDateTime.now());
        invoice.setUpdatedAt(ZonedDateTime.now());

        // Пока просто сохраняем, кто создал (в shipped_by запишем позже)
        // invoice.setShippedBy(username);

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

        // 1. Меняем статус накладной
        invoice.setStatus("shipped");
        invoice.setShippedBy(shippedBy);
        invoice.setUpdatedAt(ZonedDateTime.now());

        // Снимаем блокировку (если она была)
        invoice.setLockedBy(null);
        invoice.setLockedAt(null);

        // 2. Проходимся по строкам накладной
        for (InvoiceItem item : invoice.getItems()) {
            // Если это конкретное оборудование с QR
            if (item.getUnitId() != null) {
                Unit unit = unitRepository.findById(item.getUnitId())
                        .orElseThrow(() -> new RuntimeException("Оборудование не найдено"));

                if (!"warehouse".equals(unit.getStatus())) {
                    throw new RuntimeException("Оборудование " + unit.getSerialNumber() + " недоступно (статус: " + unit.getStatus() + ")");
                }

                // Меняем статус оборудования
                String oldStatus = unit.getStatus();
                unit.setStatus("client"); // Или "rented", как решите
                unit.setUpdatedAt(ZonedDateTime.now());
                unitRepository.save(unit);

                // Пишем в историю
                Movement movement = new Movement();
                movement.setUnitId(unit.getId());
                movement.setInvoiceId(invoice.getId());
                movement.setFromStatus(oldStatus);
                movement.setToStatus("client");
                movement.setComment("Отгрузка по накладной №" + invoice.getId());
                movementRepository.save(movement);
            }

            // TODO: Обработка мелкого оборудования (bulk_item) и ящиков — сделаем позже

            // Помечаем, сколько отгрузили
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

        int totalShipped = 0;
        int totalReturnedNow = 0;

        for (InvoiceItem item : invoice.getItems()) {
            // Если это конкретное оборудование с QR
            if (item.getUnitId() != null) {
                totalShipped += item.getQtyShipped();

                // Если это оборудование есть в списке возвращённых
                if (receivedUnitIds.contains(item.getUnitId())) {
                    // Проверяем, не вернули ли его уже (защита от двойной приёмки)
                    if (item.getQtyReturned() > 0) {
                        continue; // Уже принято ранее
                    }

                    Unit unit = unitRepository.findById(item.getUnitId())
                            .orElseThrow(() -> new RuntimeException("Оборудование не найдено"));

                    String oldStatus = unit.getStatus();
                    unit.setStatus("warehouse"); // Возвращаем на склад
                    unit.setUpdatedAt(ZonedDateTime.now());
                    unitRepository.save(unit);

                    // Пишем в историю
                    Movement movement = new Movement();
                    movement.setUnitId(unit.getId());
                    movement.setInvoiceId(invoice.getId());
                    movement.setFromStatus(oldStatus);
                    movement.setToStatus("warehouse");
                    movement.setComment("Возврат по накладной №" + invoice.getId());
                    movementRepository.save(movement);

                    // Помечаем в накладной, что эту единицу вернули
                    item.setQtyReturned(item.getQtyShipped());
                    totalReturnedNow += item.getQtyShipped();
                }
            }
            // TODO: Обработка мелкого оборудования (bulk_item) — сделаем позже
        }

        // Проверяем, всё ли вернули
        boolean allReturned = invoice.getItems().stream()
                .filter(i -> i.getUnitId() != null) // Пока считаем только QR-оборудование
                .allMatch(i -> i.getQtyReturned() >= i.getQtyShipped());

        if (allReturned) {
            invoice.setStatus("returned");
        } else {
            invoice.setStatus("partially_returned");
        }

        invoice.setUpdatedAt(ZonedDateTime.now());
        invoiceRepository.save(invoice);
    }
}