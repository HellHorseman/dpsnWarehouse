package pro.dpsn.warehouse.service;

import pro.dpsn.warehouse.dto.CreateUserRequest;
import pro.dpsn.warehouse.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    Optional<User> findByPhone(String phone);
    void createUser(CreateUserRequest request);
    List<User> getAllUsers();
}