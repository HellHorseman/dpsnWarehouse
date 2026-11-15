package pro.dpsn.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.dpsn.warehouse.dto.BoxResponse;
import pro.dpsn.warehouse.dto.UnitResponse;
import pro.dpsn.warehouse.model.Box;
import pro.dpsn.warehouse.model.Unit;
import pro.dpsn.warehouse.repo.BoxRepository;
import pro.dpsn.warehouse.repo.UnitRepository;
import pro.dpsn.warehouse.service.BoxService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoxServiceImpl implements BoxService {

    private final BoxRepository boxRepository;
    private final UnitRepository unitRepository;

    @Override
    @Transactional
    public void receiveBox(Long boxId) {
        Box box = boxRepository.findById(boxId)
                .orElseThrow(() -> new RuntimeException("Ящик не найден"));

        if (!"in_transit".equals(box.getStatus())) {
            throw new RuntimeException("Ящик не в пути (статус: " + box.getStatus() + ")");
        }

        box.setStatus("on_stock");
        boxRepository.save(box);
    }

    @Override
    public List<BoxResponse> getAllBoxes() {
        return boxRepository.findAll().stream().map(box -> {
            BoxResponse dto = new BoxResponse();
            dto.setId(box.getId());
            dto.setCode(box.getCode());
            dto.setQrCode(box.getQrCode());
            dto.setType(box.getType() != null ? box.getType().name() : "UNKNOWN");
            dto.setStatus(box.getStatus());
            dto.setMaxCells(box.getMaxCells());
            return dto;
        }).toList();
    }

    @Override
    public BoxResponse getBoxById(Long id) {
        Box box = boxRepository.findById(id).orElseThrow(() -> new RuntimeException("Ящик не найден"));
        BoxResponse dto = new BoxResponse();
        dto.setId(box.getId());
        dto.setCode(box.getCode());
        dto.setQrCode(box.getQrCode());
        dto.setType(box.getType() != null ? box.getType().name() : "UNKNOWN");
        dto.setStatus(box.getStatus());
        dto.setMaxCells(box.getMaxCells());

        // Находим все юниты, у которых current_box_id = id
        List<Unit> units = unitRepository.findAll().stream()
                .filter(u -> id.equals(u.getCurrentBoxId()))
                .toList();

        dto.setUnits(units.stream().map(unit -> {
            UnitResponse u = new UnitResponse();
            u.setId(unit.getId());
            u.setSerialNumber(unit.getSerialNumber());
            u.setQrCode(unit.getQrCode());
            u.setStatus(unit.getStatus());
            u.setModelName(unit.getModel().getName());
            return u;
        }).toList());

        return dto;
    }
}