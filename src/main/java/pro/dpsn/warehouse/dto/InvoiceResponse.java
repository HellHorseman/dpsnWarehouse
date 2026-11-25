package pro.dpsn.warehouse.dto;

import lombok.Data;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class InvoiceResponse {
    private Long id;
    private String customer;
    private String responsible;
    private String status;
    private ZonedDateTime createdAt;
    private String shippedBy;
    private String creatorName;
    private List<ItemDto> items;

    @Data
    public static class ItemDto {
        private Long id;
        private Long unitId;
        private String unitName;
        private Long bulkItemId;
        private String bulkItemName;
        private Long boxId;        // <-- Добавили
        private String boxName;    // <-- Добавили
        private Integer qtyPlanned;
        private Integer qtyShipped;
        private Integer qtyReturned;
    }
}