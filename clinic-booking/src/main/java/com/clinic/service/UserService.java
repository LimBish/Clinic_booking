package com.clinic.service;

import com.clinic.exception.AppException;
import com.clinic.exception.NotFoundException;
import com.clinic.model.User;
import com.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public User findByEmail(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User", email));
    }

    public User updateProfile(String email, String fullName, String phone) {
        User user = findByEmail(email);
        user.setFullName(fullName);
        user.setPhone(phone);
        return userRepo.save(user);
    }

    public void changePassword(String email, String currentPw, String newPw) {
        User user = findByEmail(email);
        if (!encoder.matches(currentPw, user.getPassword()))
            throw new AppException("Current password is incorrect");
        user.setPassword(encoder.encode(newPw));
        userRepo.save(user);
    }

    public long countAll() { return userRepo.count(); }
}
