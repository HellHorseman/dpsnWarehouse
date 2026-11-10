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

    @GetMapping
    public List<UnitResponse> getUnits(@RequestParam(required = false) String status) {
        if (status != null) {
            return unitService.getUnitsByStatus(status);
        }
        return unitService.getAllUnits();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public void updateStatus(@PathVariable Long id, @RequestBody StatusUpdateRequest request) {
        unitService.updateStatus(id, request.getStatus(), request.getComment());
    }
}