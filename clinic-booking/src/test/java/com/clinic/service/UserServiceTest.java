package com.clinic.service;

import com.clinic.exception.AppException;
import com.clinic.exception.NotFoundException;
import com.clinic.model.User;
import com.clinic.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder encoder;

    @InjectMocks private UserService userService;

    @Test
    void findByEmail_whenMissing_throwsNotFound() {
        when(userRepo.findByEmail("missing@clinic.com")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> userService.findByEmail("missing@clinic.com"));

        assertEquals("User not found with id: missing@clinic.com", ex.getMessage());
    }

    @Test
    void updateProfile_updatesFieldsAndSaves() {
        User user = new User();
        user.setEmail("patient@clinic.com");

        when(userRepo.findByEmail("patient@clinic.com")).thenReturn(Optional.of(user));
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User updated = userService.updateProfile("patient@clinic.com", "New Name", "999");

        assertEquals("New Name", updated.getFullName());
        assertEquals("999", updated.getPhone());
        verify(userRepo).save(user);
    }

    @Test
    void changePassword_whenCurrentPasswordDoesNotMatch_throwsAppException() {
        User user = new User();
        user.setEmail("patient@clinic.com");
        user.setPassword("encoded-old");

        when(userRepo.findByEmail("patient@clinic.com")).thenReturn(Optional.of(user));
        when(encoder.matches("wrong", "encoded-old")).thenReturn(false);

        AppException ex = assertThrows(AppException.class,
                () -> userService.changePassword("patient@clinic.com", "wrong", "new-pass"));

        assertEquals("Current password is incorrect", ex.getMessage());
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void changePassword_whenCurrentPasswordMatches_encodesAndSaves() {
        User user = new User();
        user.setEmail("patient@clinic.com");
        user.setPassword("encoded-old");

        when(userRepo.findByEmail("patient@clinic.com")).thenReturn(Optional.of(user));
        when(encoder.matches("old-pass", "encoded-old")).thenReturn(true);
        when(encoder.encode("new-pass")).thenReturn("encoded-new");

        userService.changePassword("patient@clinic.com", "old-pass", "new-pass");

        assertEquals("encoded-new", user.getPassword());
        verify(userRepo).save(user);
    }

    @Test
    void countAll_returnsRepositoryCount() {
        when(userRepo.count()).thenReturn(42L);
        assertEquals(42L, userService.countAll());
    }
}