package com.clinic.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // ── AppException ──────────────────────────────────────────────────────────

    @Test
    void handleAppException_forApi_returnsBadRequestBody() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/book");

        Object res = handler.handle(new AppException("bad request"), req);

        assertTrue(res instanceof ResponseEntity<?>);
        assertEquals(400, ((ResponseEntity<?>) res).getStatusCode().value());
    }

    @Test
    void handleAppException_forWeb_returnsErrorView() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/patient/book");

        Object res = handler.handle(new AppException("not allowed"), req);

        assertTrue(res instanceof ModelAndView);
        ModelAndView mv = (ModelAndView) res;
        assertEquals("error", mv.getViewName());
        assertEquals(400, mv.getStatus().value());
    }

    @Test
    void handleAppException_withJsonAcceptHeader_treatsAsApi() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/patient/book");
        req.addHeader("Accept", "application/json");

        Object res = handler.handle(new AppException("err"), req);

        assertTrue(res instanceof ResponseEntity<?>);
    }

    // ── NotFoundException ─────────────────────────────────────────────────────

    @Test
    void handleNotFound_forWeb_returnsErrorView() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/patient/profile");

        Object res = handler.handle(new NotFoundException("User", 1L), req);

        assertTrue(res instanceof ModelAndView);
        ModelAndView mv = (ModelAndView) res;
        assertEquals("error", mv.getViewName());
        assertEquals(404, mv.getStatus().value());
    }

    @Test
    void handleNotFound_forApi_returns404Body() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/patient/99");

        Object res = handler.handle(new NotFoundException("User", 99L), req);

        assertTrue(res instanceof ResponseEntity<?>);
        assertEquals(404, ((ResponseEntity<?>) res).getStatusCode().value());
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void handleValidation_forApi_joinsMessages() throws NoSuchMethodException {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "email", "Invalid email"));
        binding.addError(new FieldError("obj", "password", "Too short"));

        MethodParameter parameter = new MethodParameter(
                Dummy.class.getDeclaredMethod("dummy", String.class), 0);

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(parameter, binding);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/register");

        Object res = handler.handle(ex, req);
        ResponseEntity<?> entity = (ResponseEntity<?>) res;

        assertEquals(400, entity.getStatusCode().value());
        String body = entity.getBody().toString();
        assertTrue(body.contains("Invalid email"));
        assertTrue(body.contains("Too short"));
    }

    @Test
    void handleValidation_forWeb_returnsErrorView() throws NoSuchMethodException {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "email", "Required"));

        MethodParameter parameter = new MethodParameter(
                Dummy.class.getDeclaredMethod("dummy", String.class), 0);

        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(parameter, binding);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/register");

        Object res = handler.handle(ex, req);

        assertTrue(res instanceof ModelAndView);
        assertEquals(400, ((ModelAndView) res).getStatus().value());
    }

    // ── BadCredentialsException ───────────────────────────────────────────────

    @Test
    void handleBadCredentials_forApi_returns401() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/login");

        Object res = handler.handle(new BadCredentialsException("bad creds"), req);

        assertTrue(res instanceof ResponseEntity<?>);
        assertEquals(401, ((ResponseEntity<?>) res).getStatusCode().value());
    }

    @Test
    void handleBadCredentials_forWeb_returnsErrorView() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/login");

        Object res = handler.handle(new BadCredentialsException("bad creds"), req);

        assertTrue(res instanceof ModelAndView);
        assertEquals(401, ((ModelAndView) res).getStatus().value());
    }

    // ── AccessDeniedException ─────────────────────────────────────────────────

    @Test
    void handleAccessDenied_forApi_returns403() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/admin");

        Object res = handler.handle(new AccessDeniedException("denied"), req);

        assertTrue(res instanceof ResponseEntity<?>);
        assertEquals(403, ((ResponseEntity<?>) res).getStatusCode().value());
    }

    @Test
    void handleAccessDenied_forWeb_returnsErrorView() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/admin/dashboard");

        Object res = handler.handle(new AccessDeniedException("denied"), req);

        assertTrue(res instanceof ModelAndView);
        assertEquals(403, ((ModelAndView) res).getStatus().value());
    }

    // ── Generic Exception ─────────────────────────────────────────────────────

    @Test
    void handleGeneric_forWeb_returns500View() {
        HttpServletRequest req = new MockHttpServletRequest("GET", "/x");

        Object res = handler.handle(new RuntimeException("boom"), req);

        assertTrue(res instanceof ModelAndView);
        assertEquals(500, ((ModelAndView) res).getStatus().value());
    }

    @Test
    void handleGeneric_forApi_returns500Body() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/something");

        Object res = handler.handle(new RuntimeException("crash"), req);

        assertTrue(res instanceof ResponseEntity<?>);
        assertEquals(500, ((ResponseEntity<?>) res).getStatusCode().value());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static class Dummy {
        public void dummy(String value) { }
    }
}