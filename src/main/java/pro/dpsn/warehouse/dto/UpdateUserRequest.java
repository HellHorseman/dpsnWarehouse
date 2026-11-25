package pro.dpsn.warehouse.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String fullName;
    private String role;
    private Boolean active;
    private String pin; // Если пришел — меняем, если null — оставляем старый
}