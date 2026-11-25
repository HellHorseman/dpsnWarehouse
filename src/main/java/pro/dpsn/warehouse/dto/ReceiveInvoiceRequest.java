package pro.dpsn.warehouse.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ReceiveInvoiceRequest {
    private List<Long> unitIds; // Список ID оборудования, которое вернули
    private List<Long> scannedUnitIds; // ID сканированных QR
    private Map<Long, Integer> bulkReturns; // Ключ: ID мелкого, Значение: сколько вернули
}