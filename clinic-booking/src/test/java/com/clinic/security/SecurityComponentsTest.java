package com.clinic.security;

import com.clinic.model.User;
import com.clinic.model.enums.Role;
import com.clinic.repository.UserRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityComponentsTest {

    @Mock private UserRepository userRepository;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks private UserDetailsServiceImpl userDetailsServiceImpl;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userDetailsServiceImpl_whenUserMissing_throws() {
        when(userRepository.findByEmail("missing@clinic.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsServiceImpl.loadUserByUsername("missing@clinic.com"));
    }

    @Test
    void userDetailsServiceImpl_whenUserExists_returnsRoleAuthority() {
        User user = new User();
        user.setEmail("admin@clinic.com");
        user.setPassword("encoded");
        user.setRole(Role.ADMIN);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        UserDetails details = userDetailsServiceImpl.loadUserByUsername(user.getEmail());

        assertEquals("admin@clinic.com", details.getUsername());
        assertTrue(details.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void jwtUtil_generateExtractAndValidate_roundTrip() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60000L);

        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername("user@clinic.com")
                .password("x")
                .roles("PATIENT")
                .build();

        String token = jwtUtil.generate(details);

        assertEquals("user@clinic.com", jwtUtil.extractEmail(token));
        assertTrue(jwtUtil.validate(token, details));
    }

    @Test
    void jwtFilter_withValidBearer_setsAuthentication() throws Exception {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60000L);

        UserDetails details = org.springframework.security.core.userdetails.User
                .withUsername("user@clinic.com")
                .password("x")
                .roles("PATIENT")
                .build();

        String token = jwtUtil.generate(details);
        JwtFilter filter = new JwtFilter(jwtUtil, userDetailsService);
        when(userDetailsService.loadUserByUsername("user@clinic.com")).thenReturn(details);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertNull(req.getAttribute("jwt_error"));
    }

    @Test
    void jwtFilter_withInvalidToken_setsErrorAttribute() throws Exception {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60000L);
        JwtFilter filter = new JwtFilter(jwtUtil, userDetailsService);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer not-a-jwt");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, new MockFilterChain());

        assertEquals("incorrect JWT token", req.getAttribute("jwt_error"));
    }
}