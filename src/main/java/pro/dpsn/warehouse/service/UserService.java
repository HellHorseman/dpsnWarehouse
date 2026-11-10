package pro.dpsn.warehouse.service;

import pro.dpsn.warehouse.model.User;
import java.util.Optional;

public interface UserService {
    Optional<User> findByPhone(String phone);
}