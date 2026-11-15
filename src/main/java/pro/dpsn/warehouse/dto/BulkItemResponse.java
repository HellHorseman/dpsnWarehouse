package pro.dpsn.warehouse.dto;

import lombok.Data;

@Data
public class BulkItemResponse {
    private Long id;
    private String name;
    private Integer qtyOnHand;
}