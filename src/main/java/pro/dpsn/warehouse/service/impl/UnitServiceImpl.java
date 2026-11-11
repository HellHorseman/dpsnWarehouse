package pro.dpsn.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.dpsn.warehouse.dto.UnitResponse;
import pro.dpsn.warehouse.model.Movement;
import pro.dpsn.warehouse.model.Unit;
import pro.dpsn.warehouse.repo.MovementRepository;
import pro.dpsn.warehouse.repo.UnitRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

import pro.dpsn.warehouse.service.UnitService;

@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {
    private final UnitRepository unitRepository;
    private final MovementRepository movementRepository;

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

    @Override
    @Transactional // Важно! Если что-то упадет, транзакция откатится
    public void updateStatus(Long unitId, String newStatus, String comment) {
        Unit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new RuntimeException("Оборудование не найдено"));

        String oldStatus = unit.getStatus();

        // Если статус не изменился, ничего не делаем
        if (oldStatus.equals(newStatus)) {
            return;
        }

        // 1. Обновляем статус
        unit.setStatus(newStatus);
        unit.setUpdatedAt(ZonedDateTime.now());
        unitRepository.save(unit);

        // 2. Пишем в историю
        Movement movement = new Movement();
        movement.setUnitId(unit.getId());
        movement.setFromStatus(oldStatus);
        movement.setToStatus(newStatus);
        movement.setComment(comment);
        movement.setCreatedAt(ZonedDateTime.now());
        movementRepository.save(movement);
    }

    @Override
    public UnitResponse getUnitByQr(String qrCode) {
        Unit unit = unitRepository.findByQrCode(qrCode)
                .orElseThrow(() -> new RuntimeException("Оборудование не найдено"));

        UnitResponse dto = new UnitResponse();
        dto.setId(unit.getId());
        dto.setSerialNumber(unit.getSerialNumber());
        dto.setQrCode(unit.getQrCode());
        dto.setStatus(unit.getStatus());
        dto.setModelName(unit.getModel().getName());
        return dto;
    }

    @Override
    public List<UnitResponse> getFilteredUnits(String status, String modelName) {
        return unitRepository.findWithFilters(status, modelName).stream()
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