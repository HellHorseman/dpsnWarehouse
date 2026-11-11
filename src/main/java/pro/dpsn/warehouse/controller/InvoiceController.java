package pro.dpsn.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pro.dpsn.warehouse.dto.*;
import pro.dpsn.warehouse.model.Invoice;
import pro.dpsn.warehouse.service.InvoiceService;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public InvoiceResponse createInvoice(@RequestBody CreateInvoiceRequest request, Authentication authentication) {
        Invoice invoice = invoiceService.createInvoice(request, authentication.getName());
        return invoiceService.getInvoiceById(invoice.getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public InvoiceResponse getInvoice(@PathVariable Long id) {
        return invoiceService.getInvoiceById(id);
    }

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public void shipInvoice(@PathVariable Long id,
                            @RequestBody ShipInvoiceRequest request,
                            Authentication authentication) {
        invoiceService.shipInvoice(id, request.getShippedBy(), authentication.getName());
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public void receiveInvoice(@PathVariable Long id,
                               @RequestBody ReceiveInvoiceRequest request,
                               Authentication authentication) {
        invoiceService.receiveInvoice(id, request.getUnitIds(), authentication.getName());
    }

    @DeleteMapping("/{invoiceId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public void removeItem(@PathVariable Long invoiceId, @PathVariable Long itemId) {
        invoiceService.removeItemFromInvoice(invoiceId, itemId);
    }

    @GetMapping("/{id}/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ResponseEntity<byte[]> exportInvoice(@PathVariable Long id) {
        byte[] excel = invoiceService.exportInvoiceToExcel(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice_" + id + ".xlsx")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(excel);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public ImportResult importInvoice(@RequestParam("file") MultipartFile file, Authentication authentication) {
        return invoiceService.importInvoiceFromExcel(file, authentication.getName());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceService.getAllInvoices();
    }
}