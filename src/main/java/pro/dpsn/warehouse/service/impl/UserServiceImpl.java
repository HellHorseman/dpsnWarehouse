package pro.dpsn.warehouse.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pro.dpsn.warehouse.dto.CreateUserRequest;
import pro.dpsn.warehouse.dto.UpdateUserRequest;
import pro.dpsn.warehouse.model.User;
import pro.dpsn.warehouse.repo.UserRepository;
import pro.dpsn.warehouse.service.UserService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    @Override
    public void createUser(CreateUserRequest request) {
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new RuntimeException("Пользователь с таким телефоном уже существует");
        }

        User user = new User();
        user.setPhone(request.getPhone());
        user.setFullName(request.getFullName());
        user.setPinHash(passwordEncoder.encode(request.getPin())); // Хешируем PIN!
        user.setRole(request.getRole());
        user.setActive(true);

        userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    @Transactional
    public void updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getActive() != null) user.setActive(request.getActive());

        // Меняем PIN только если он передан (не пустой)
        if (request.getPin() != null && !request.getPin().trim().isEmpty()) {
            user.setPinHash(passwordEncoder.encode(request.getPin()));
        }

        userRepository.save(user);
    }
}