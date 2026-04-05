package com.clinic.service;

import com.clinic.dto.Dtos.AuthResponse;
import com.clinic.dto.Dtos.LoginRequest;
import com.clinic.dto.Dtos.RegisterRequest;
import com.clinic.exception.AppException;
import com.clinic.exception.NotFoundException;
import com.clinic.model.User;
import com.clinic.model.enums.Role;
import com.clinic.repository.UserRepository;
import com.clinic.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private PasswordEncoder encoder;
    @Mock private AuthenticationManager authManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks private AuthService authService;

    @Test
    void register_whenEmailExists_throwsAppException() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("taken@clinic.com");

        when(userRepo.existsByEmail("taken@clinic.com")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.register(req));
        assertEquals("An account with this email already exists", ex.getMessage());
    }

    @Test
    void register_whenValidRequest_savesPatientWithEncodedPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane Doe");
        req.setEmail("jane@clinic.com");
        req.setPassword("plain-pass");
        req.setPhone("1234567890");

        when(userRepo.existsByEmail(req.getEmail())).thenReturn(false);
        when(encoder.encode("plain-pass")).thenReturn("encoded-pass");
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User saved = authService.register(req);

        assertEquals("Jane Doe", saved.getFullName());
        assertEquals("encoded-pass", saved.getPassword());
        assertEquals(Role.PATIENT, saved.getRole());
        verify(userRepo).save(any(User.class));
    }

    @Test
    void login_whenUserRecordMissing_throwsNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("missing@clinic.com");
        req.setPassword("pw");

        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername(req.getEmail())
                .password("x")
                .roles("PATIENT")
                .build();

        when(userDetailsService.loadUserByUsername(req.getEmail())).thenReturn(details);
        when(userRepo.findByEmail(req.getEmail())).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> authService.login(req));
        assertEquals("User not found with id: missing@clinic.com", ex.getMessage());
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_whenValid_returnsJwtRoleAndName() {
        LoginRequest req = new LoginRequest();
        req.setEmail("doctor@clinic.com");
        req.setPassword("pw");

        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername(req.getEmail())
                .password("x")
                .roles("DOCTOR")
                .build();

        User user = new User();
        user.setEmail(req.getEmail());
        user.setRole(Role.DOCTOR);
        user.setFullName("Dr. Carter");

        when(userDetailsService.loadUserByUsername(req.getEmail())).thenReturn(details);
        when(userRepo.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(jwtUtil.generate(details)).thenReturn("token-123");

        AuthResponse response = authService.login(req);

        assertEquals("token-123", response.getToken());
        assertEquals("DOCTOR", response.getRole());
        assertEquals("Dr. Carter", response.getName());
    }

    @Test
    void loginBasicCredentials_delegatesToLogin() {
        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername("patient@clinic.com")
                .password("x")
                .roles("PATIENT")
                .build();

        User user = new User();
        user.setEmail("patient@clinic.com");
        user.setRole(Role.PATIENT);
        user.setFullName("Patient One");

        when(userDetailsService.loadUserByUsername("patient@clinic.com")).thenReturn(details);
        when(userRepo.findByEmail("patient@clinic.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generate(details)).thenReturn("jwt-basic");

        AuthResponse response = authService.loginBasicCredentials("patient@clinic.com", "pw");

        assertEquals("jwt-basic", response.getToken());
        verify(authManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}