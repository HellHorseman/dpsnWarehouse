package pro.dpsn.warehouse.dto;

import lombok.Data;

@Data
public class StatusUpdateRequest {
    private String status; // warehouse, rented, service, client, written_off
    private String comment;
}