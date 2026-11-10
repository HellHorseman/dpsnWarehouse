package pro.dpsn.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pro.dpsn.warehouse.dto.UnitResponse;
import pro.dpsn.warehouse.repo.UnitRepository;
import java.util.List;
import java.util.stream.Collectors;

import pro.dpsn.warehouse.service.UnitService;

@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {
    private final UnitRepository unitRepository;

    @Override
    public List<UnitResponse> getAllUnits() {
        return unitRepository.findAll().stream()
                .map(unit -> {
                    UnitResponse dto = new UnitResponse();
                    dto.setId(unit.getId());
                    dto.setSerialNumber(unit.getSerialNumber());
                    dto.setQrCode(unit.getQrCode());
                    dto.setStatus(unit.getStatus());
                    dto.setModelName(unit.getModel().getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<UnitResponse> getUnitsByStatus(String status) {
        return unitRepository.findByStatus(status).stream()
                .map(unit -> {
                    UnitResponse dto = new UnitResponse();
                    dto.setId(unit.getId());
                    dto.setSerialNumber(unit.getSerialNumber());
                    dto.setQrCode(unit.getQrCode());
                    dto.setStatus(unit.getStatus());
                    dto.setModelName(unit.getModel().getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}