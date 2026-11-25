package pro.dpsn.warehouse.service;

import org.springframework.web.multipart.MultipartFile;
import pro.dpsn.warehouse.dto.CreateInvoiceRequest;
import pro.dpsn.warehouse.dto.ImportResult;
import pro.dpsn.warehouse.dto.InvoiceResponse;
import pro.dpsn.warehouse.model.Invoice;

import java.util.List;

public interface InvoiceService {
    Invoice createInvoice(CreateInvoiceRequest request, String username);
    InvoiceResponse getInvoiceById(Long id);
    void shipInvoice(Long invoiceId, String username);
    void receiveInvoice(Long invoiceId, List<Long> receivedUnitIds, String username);
    void removeItemFromInvoice(Long invoiceId, Long itemId);
    byte[] exportInvoiceToExcel(Long invoiceId);
    ImportResult importInvoiceFromExcel(MultipartFile file, String username);
    List<InvoiceResponse> getAllInvoices();
    void addItemToInvoice(Long invoiceId, CreateInvoiceRequest.ItemRequest itemRequest);
    void updateItemQty(Long invoiceId, Long itemId, Integer qty);
    void deleteDraftInvoice(Long invoiceId);
    void updateItemBox(Long invoiceId, Long itemId, Long boxId);
}