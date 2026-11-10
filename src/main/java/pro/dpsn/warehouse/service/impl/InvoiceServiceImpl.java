package pro.dpsn.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.dpsn.warehouse.dto.CreateInvoiceRequest;
import pro.dpsn.warehouse.dto.InvoiceResponse;
import pro.dpsn.warehouse.model.Invoice;
import pro.dpsn.warehouse.model.InvoiceItem;
import pro.dpsn.warehouse.repo.InvoiceRepository;
import pro.dpsn.warehouse.repo.UnitRepository;
import pro.dpsn.warehouse.service.InvoiceService;

import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UnitRepository unitRepository;

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
}