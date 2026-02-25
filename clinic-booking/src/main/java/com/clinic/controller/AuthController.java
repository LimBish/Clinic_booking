package com.clinic.controller;

import com.clinic.dto.Dtos.*;
import com.clinic.exception.AppException;
import com.clinic.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/")
    public String index() { return "redirect:/login"; }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    // Web form: on AppException (duplicate email) stay on page with message
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest req, Model model) {
        try {
            authService.register(req);
            return "redirect:/login?registered";
        } catch (AppException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    // Web form: on bad credentials stay on login page with message
    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest req, Model model, HttpServletResponse res) {
        try {
            AuthResponse auth = authService.login(req);
            Cookie cookie = new Cookie("jwt", auth.getToken());
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(86400);
            res.addCookie(cookie);
            return switch (auth.getRole()) {
                case "ADMIN"  -> "redirect:/admin/dashboard";
                case "DOCTOR" -> "redirect:/doctor/dashboard";
                default       -> "redirect:/patient/dashboard";
            };
        } catch (BadCredentialsException e) {
            model.addAttribute("error", "Invalid email or password");
            return "login";
        }
    }

    // REST API — exceptions propagate to GlobalExceptionHandler
    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<String> apiRegister(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok("Registered successfully");
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<AuthResponse> apiLogin(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/api/auth/basic-login")
    @ResponseBody
    public ResponseEntity<?> apiBasicLogin(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return ResponseEntity.status(401).body(new ApiMessage("Missing Basic Authorization header"));
        }

        String base64 = authHeader.substring(6).trim();
        String decoded;
        try {
            decoded = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(new ApiMessage("Invalid Basic credentials format"));
        }

        int split = decoded.indexOf(':');
        if (split < 1) {
            return ResponseEntity.status(401).body(new ApiMessage("Invalid Basic credentials format"));
        }

        String email = decoded.substring(0, split);
        String password = decoded.substring(split + 1);
        AuthResponse auth = authService.loginBasicCredentials(email, password);
        return ResponseEntity.ok(new TokenResponse(auth.getToken()));
    }

    public record ApiMessage(String message) {}
    public record TokenResponse(String token) {}
}
