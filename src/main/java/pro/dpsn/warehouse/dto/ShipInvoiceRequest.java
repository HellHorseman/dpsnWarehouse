package pro.dpsn.warehouse.dto;

import lombok.Data;

@Data
public class ShipInvoiceRequest {
    private String shippedBy; // ФИО или телефон того, кто нажал кнопку
}