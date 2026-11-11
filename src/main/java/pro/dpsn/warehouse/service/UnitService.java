package pro.dpsn.warehouse.service;

import pro.dpsn.warehouse.dto.UnitResponse;
import java.util.List;

public interface UnitService {
    List<UnitResponse> getAllUnits();
    List<UnitResponse> getUnitsByStatus(String status);
    void updateStatus(Long unitId, String newStatus, String comment);
    UnitResponse getUnitByQr(String qrCode);
}