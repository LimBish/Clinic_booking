package com.clinic.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAppException_forApi_returnsBadRequestBody() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/book");

        Object res = handler.handle(new AppException("bad request"), req);

        assertTrue(res instanceof ResponseEntity<?>);
        ResponseEntity<?> entity = (ResponseEntity<?>) res;
        assertEquals(400, entity.getStatusCode().value());
    }

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
    void handleValidation_joinsMessages() throws NoSuchMethodException {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "email", "Invalid email"));
        binding.addError(new FieldError("obj", "password", "Too short"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(
                Dummy.class.getDeclaredMethod("dummy", String.class), binding);

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
    void handleGeneric_forWeb_returns500View() {
        HttpServletRequest req = new MockHttpServletRequest("GET", "/x");

        Object res = handler.handle(new RuntimeException("boom"), req);

        ModelAndView mv = (ModelAndView) res;
        assertEquals(500, mv.getStatus().value());
    }

    private static class Dummy {
        public void dummy(String value) { }
    }
}