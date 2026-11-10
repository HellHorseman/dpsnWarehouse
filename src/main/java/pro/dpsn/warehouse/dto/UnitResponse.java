package pro.dpsn.warehouse.dto;

import lombok.Data;

@Data
public class UnitResponse {
    private Long id;
    private String serialNumber;
    private String qrCode;
    private String status;
    private String modelName; // Вместо всего объекта Model отдаем только имя
}