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
    private List<ItemDto> items;

    @Data
    public static class ItemDto {
        private Long id;
        private Long unitId;
        private Integer qtyPlanned;
        private Integer qtyShipped;
    }
}