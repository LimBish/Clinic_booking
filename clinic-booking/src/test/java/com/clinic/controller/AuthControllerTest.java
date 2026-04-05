package com.clinic.controller;

import com.clinic.dto.Dtos.AuthResponse;
import com.clinic.dto.Dtos.LoginRequest;
import com.clinic.dto.Dtos.RegisterRequest;
import com.clinic.exception.AppException;
import com.clinic.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.ui.Model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private Model model;
    @Mock private HttpServletResponse response;

    @InjectMocks private AuthController authController;

    @Test
    void register_whenDuplicate_staysOnRegisterPage() {
        RegisterRequest req = new RegisterRequest();
        when(authService.register(req)).thenThrow(new AppException("duplicate"));

        String view = authController.register(req, model);

        assertEquals("register", view);
        verify(model).addAttribute("error", "duplicate");
    }

    @Test
    void login_whenDoctorRole_redirectsAndSetsCookie() {
        LoginRequest req = new LoginRequest();
        req.setEmail("doc@clinic.com");
        req.setPassword("pw");
        when(authService.login(req)).thenReturn(AuthResponse.of("jwt", "DOCTOR", "Doc"));

        String view = authController.login(req, model, response);

        assertEquals("redirect:/doctor/dashboard", view);

        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(captor.capture());
        Cookie cookie = captor.getValue();
        assertEquals("jwt", cookie.getName());
        assertEquals("jwt", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
    }

    @Test
    void login_whenBadCredentials_returnsLoginWithError() {
        LoginRequest req = new LoginRequest();
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("bad"));

        String view = authController.login(req, model, response);

        assertEquals("login", view);
        verify(model).addAttribute("error", "Invalid email or password");
    }

    @Test
    void apiBasicLogin_whenHeaderMissing_returnsUnauthorized() {
        var res = authController.apiBasicLogin(null);

        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void apiBasicLogin_whenInvalidBase64_returnsUnauthorized() {
        var res = authController.apiBasicLogin("Basic !!!");

        assertEquals(401, res.getStatusCode().value());
    }

    @Test
    void apiBasicLogin_whenValid_returnsToken() {
        String creds = Base64.getEncoder().encodeToString("a@b.com:pw".getBytes(StandardCharsets.UTF_8));
        when(authService.loginBasicCredentials("a@b.com", "pw")).thenReturn(AuthResponse.of("jwt-token", "PATIENT", "A"));

        var res = authController.apiBasicLogin("Basic " + creds);

        assertEquals(200, res.getStatusCode().value());
        AuthController.TokenResponse body = (AuthController.TokenResponse) res.getBody();
        assertEquals("jwt-token", body.token());
    }
}