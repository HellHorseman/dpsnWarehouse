package pro.dpsn.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pro.dpsn.warehouse.dto.StatusUpdateRequest;
import pro.dpsn.warehouse.dto.UnitResponse;
import pro.dpsn.warehouse.service.UnitService;
import java.util.List;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

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
}