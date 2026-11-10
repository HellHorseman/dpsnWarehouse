package pro.dpsn.warehouse.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReceiveInvoiceRequest {
    private List<Long> unitIds; // Список ID оборудования, которое вернули
}