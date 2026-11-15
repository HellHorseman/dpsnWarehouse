package pro.dpsn.warehouse.dto;

import lombok.Data;
import java.util.List;

@Data
public class BoxResponse {
    private Long id;
    private String code;
    private String qrCode;
    private String type;
    private String status;
    private Integer maxCells;
    // Список единиц, которые сейчас физически в ящике (по current_box_id)
    private List<UnitResponse> units;
}