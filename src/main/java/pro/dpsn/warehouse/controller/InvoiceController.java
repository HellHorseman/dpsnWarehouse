package pro.dpsn.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pro.dpsn.warehouse.dto.CreateInvoiceRequest;
import pro.dpsn.warehouse.dto.InvoiceResponse;
import pro.dpsn.warehouse.dto.ShipInvoiceRequest;
import pro.dpsn.warehouse.model.Invoice;
import pro.dpsn.warehouse.service.InvoiceService;

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
}