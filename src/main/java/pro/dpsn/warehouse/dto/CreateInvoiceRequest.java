package pro.dpsn.warehouse.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateInvoiceRequest {
    private String customer;      // Заказчик
    private String responsible;   // Ответственный
    private List<ItemRequest> items;

    @Data
    public static class ItemRequest {
        private Long unitId;        // Если это конкретный серийник
        private Long bulkItemId;    // Если мелкое без QR (пока null)
        private Long boxId;         // Если в ящике (пока null)
        private Integer qtyPlanned; // Количество (по умолчанию 1)
    }
}