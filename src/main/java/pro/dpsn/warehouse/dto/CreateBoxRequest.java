package pro.dpsn.warehouse.dto;

import lombok.Data;

@Data
public class CreateBoxRequest {
    private String code;
    private String qrCode;
    private String type; // BULK_CONTAINER, STATIC_KIT, FIXED_BAG, FIXED_BAG_OPTIONAL
    private Integer maxCells; // Для сумок
}