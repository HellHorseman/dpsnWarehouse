package pro.dpsn.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pro.dpsn.warehouse.model.Unit;
import pro.dpsn.warehouse.repo.UnitRepository;
import pro.dpsn.warehouse.service.QrCodeService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class QrController {

    private final UnitRepository unitRepository;
    private final QrCodeService qrCodeService;

    @GetMapping("/{id}/qr")
    public ResponseEntity<byte[]> getQrCode(@PathVariable Long id) {
        try {
            Unit unit = unitRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Оборудование не найдено"));

            byte[] qrImage = qrCodeService.generateQrCode(unit.getQrCode(), 250, 250);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/png")
                    .body(qrImage);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации QR: " + e.getMessage());
        }
    }
}
