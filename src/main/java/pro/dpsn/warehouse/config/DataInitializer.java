package pro.dpsn.warehouse.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pro.dpsn.warehouse.model.User;
import pro.dpsn.warehouse.repo.UserRepository;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Проверяем, есть ли хоть один пользователь в базе
        if (userRepository.count() == 0) {
            User admin = new User();
            admin.setPhone("79000000000"); // Логин по умолчанию
            admin.setFullName("Главный Администратор");
            admin.setPinHash(passwordEncoder.encode("1234")); // PIN по умолчанию
            admin.setRole("ADMIN");
            admin.setActive(true);

            userRepository.save(admin);
            System.out.println("=====================================================");
            System.out.println("Создан администратор по умолчанию!");
            System.out.println("Логин (телефон): 79000000000");
            System.out.println("PIN: 1234");
            System.out.println("ОБЯЗАТЕЛЬНО СМЕНИТЕ ПАРОЛЬ ПОСЛЕ ПЕРВОГО ВХОДА!");
            System.out.println("=====================================================");
        }
    }
}