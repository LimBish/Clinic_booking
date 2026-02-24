package com.clinic.config;

import com.clinic.model.User;
import com.clinic.model.enums.Role;
import com.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@clinic.com")) {
            userRepository.save(new User("Admin", "admin@clinic.com",
                    encoder.encode("admin123"), null, Role.ADMIN));
        }
    }
}
