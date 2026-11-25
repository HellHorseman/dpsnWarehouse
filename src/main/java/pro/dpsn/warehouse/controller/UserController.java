package pro.dpsn.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pro.dpsn.warehouse.dto.CreateUserRequest;
import pro.dpsn.warehouse.dto.UpdateUserRequest;
import pro.dpsn.warehouse.model.User;
import pro.dpsn.warehouse.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // Только админ может создавать
    public void createUser(@RequestBody CreateUserRequest request) {
        userService.createUser(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        userService.updateUser(id, request);
    }
}