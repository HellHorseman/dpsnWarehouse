package pro.dpsn.warehouse.dto;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String phone;
    private String fullName;
    private String pin;
    private String role; // ADMIN, MANAGER, STOREKEEPER
}