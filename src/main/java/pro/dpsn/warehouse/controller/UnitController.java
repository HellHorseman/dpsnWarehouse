package pro.dpsn.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pro.dpsn.warehouse.dto.StatusUpdateRequest;
import pro.dpsn.warehouse.dto.UnitResponse;
import pro.dpsn.warehouse.model.Unit;
import pro.dpsn.warehouse.repo.UnitRepository;
import pro.dpsn.warehouse.service.QrCodeService;
import pro.dpsn.warehouse.service.UnitService;
import java.util.List;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;
    private final UnitRepository unitRepository;
    private final QrCodeService qrCodeService;

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public void updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        unitService.updateStatus(id, request.getStatus(), request.getComment());
    }

    @GetMapping("/qr/{qrCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public UnitResponse getUnitByQr(@PathVariable String qrCode) {
        return unitService.getUnitByQr(qrCode);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public List<UnitResponse> getUnits(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String modelName) {
        return unitService.getFilteredUnits(status, modelName);
    }

    @GetMapping("/{id}/qr")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<byte[]> getUnitQr(@PathVariable Long id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Оборудование не найдено"));
        try {
            byte[] qrImage = qrCodeService.generateQrCode(unit.getQrCode(), 300, 300);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/png")
                    .body(qrImage);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка генерации QR: " + e.getMessage());
        }
    }
}