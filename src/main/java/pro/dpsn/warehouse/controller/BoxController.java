package pro.dpsn.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pro.dpsn.warehouse.dto.BoxResponse;
import pro.dpsn.warehouse.service.BoxService;

import java.util.List;

@RestController
@RequestMapping("/api/boxes")
@RequiredArgsConstructor
public class BoxController {

    private final BoxService boxService;

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STOREKEEPER')")
    public void receiveBox(@PathVariable Long id) {
        boxService.receiveBox(id);
    }

    @GetMapping
    public List<BoxResponse> getAllBoxes() {
        return boxService.getAllBoxes();
    }

    @GetMapping("/{id}")
    public BoxResponse getBox(@PathVariable Long id) {
        return boxService.getBoxById(id);
    }
}