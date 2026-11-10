package pro.dpsn.warehouse.service;

import pro.dpsn.warehouse.dto.CreateInvoiceRequest;
import pro.dpsn.warehouse.dto.InvoiceResponse;
import pro.dpsn.warehouse.model.Invoice;

public interface InvoiceService {
    Invoice createInvoice(CreateInvoiceRequest request, String username);
    InvoiceResponse getInvoiceById(Long id);
    void shipInvoice(Long invoiceId, String shippedBy, String username);
}