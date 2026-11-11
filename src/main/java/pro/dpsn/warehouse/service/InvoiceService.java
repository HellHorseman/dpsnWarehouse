package pro.dpsn.warehouse.service;

import pro.dpsn.warehouse.dto.CreateInvoiceRequest;
import pro.dpsn.warehouse.dto.InvoiceResponse;
import pro.dpsn.warehouse.model.Invoice;

import java.util.List;

public interface InvoiceService {
    Invoice createInvoice(CreateInvoiceRequest request, String username);
    InvoiceResponse getInvoiceById(Long id);
    void shipInvoice(Long invoiceId, String shippedBy, String username);
    void receiveInvoice(Long invoiceId, List<Long> receivedUnitIds, String username);
    void removeItemFromInvoice(Long invoiceId, Long itemId);
    byte[] exportInvoiceToExcel(Long invoiceId);
}