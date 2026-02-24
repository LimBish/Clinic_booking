package com.clinic.service;

import com.clinic.dto.Dtos.*;
import com.clinic.exception.AppException;
import com.clinic.exception.NotFoundException;
import com.clinic.model.User;
import com.clinic.model.enums.Role;
import com.clinic.repository.UserRepository;
import com.clinic.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public User register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new AppException("An account with this email already exists");
        return userRepo.save(new User(req.getFullName(), req.getEmail(),
                encoder.encode(req.getPassword()), req.getPhone(), Role.PATIENT));
    }

    public AuthResponse login(LoginRequest req) {
        // Let BadCredentialsException propagate — handled by GlobalExceptionHandler
        authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        var userDetails = userDetailsService.loadUserByUsername(req.getEmail());
        var user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new NotFoundException("User", req.getEmail()));
        return AuthResponse.of(jwtUtil.generate(userDetails), user.getRole().name(), user.getFullName());
    }
}
