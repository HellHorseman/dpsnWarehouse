package pro.dpsn.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pro.dpsn.warehouse.dto.BoxResponse;
import pro.dpsn.warehouse.dto.CreateBoxRequest;
import pro.dpsn.warehouse.model.Box;
import pro.dpsn.warehouse.repo.BoxRepository;
import pro.dpsn.warehouse.service.BoxService;
import pro.dpsn.warehouse.service.QrCodeService;

import java.util.List;

@RestController
@RequestMapping("/api/boxes")
@RequiredArgsConstructor
public class BoxController {

    private final BoxService boxService;
    private final BoxRepository boxRepository;
    private final QrCodeService qrCodeService;

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public void receiveBox(@PathVariable Long id) {
        boxService.receiveBox(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public List<BoxResponse> getAllBoxes() {
        return boxService.getAllBoxes();
    }

    @GetMapping("/{id}")
    public BoxResponse getBox(@PathVariable Long id) {
        return boxService.getBoxById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void createBox(@RequestBody CreateBoxRequest request) {
        boxService.createBox(request);
    }

    @GetMapping("/{id}/qr")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> getBoxQr(@PathVariable Long id) {
        Box box = boxRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ящик не найден"));
        try {
            byte[] qrImage = qrCodeService.generateQrCode(box.getQrCode(), 300, 300);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/png")
                    .body(qrImage);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации QR: " + e.getMessage());
        }
    }
}