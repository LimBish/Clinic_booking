package com.clinic.config;

import com.clinic.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder encoder;

    @InjectMocks private DataInitializer dataInitializer;

    @Test
    void run_whenAdminMissing_createsAdmin() throws Exception {
        when(userRepository.existsByEmail("admin@clinic.com")).thenReturn(false);
        when(encoder.encode("admin123")).thenReturn("encoded");

        dataInitializer.run();

        verify(userRepository).save(any());
    }

    @Test
    void run_whenAdminExists_doesNothing() throws Exception {
        when(userRepository.existsByEmail("admin@clinic.com")).thenReturn(true);

        dataInitializer.run();

        verify(userRepository, never()).save(any());
    }
}