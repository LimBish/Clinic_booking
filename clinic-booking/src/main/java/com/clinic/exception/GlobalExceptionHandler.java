package com.clinic.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // ── Business errors (400) ─────────────────────────────────────────────────

    @ExceptionHandler(AppException.class)
    public Object handle(AppException ex, HttpServletRequest req) {
        log.warn("Business error [{}]: {}", req.getRequestURI(), ex.getMessage());
        return isApi(req)
                ? ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()))
                : errorView(400, ex.getMessage());
    }

    // ── Not found (404) ───────────────────────────────────────────────────────

    @ExceptionHandler(NotFoundException.class)
    public Object handle(NotFoundException ex, HttpServletRequest req) {
        log.warn("Not found [{}]: {}", req.getRequestURI(), ex.getMessage());
        return isApi(req)
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()))
                : errorView(404, ex.getMessage());
    }

    // ── Validation (400) ──────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handle(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("Validation [{}]: {}", req.getRequestURI(), msg);
        return isApi(req)
                ? ResponseEntity.badRequest().body(new ErrorResponse(msg))
                : errorView(400, msg);
    }

    // ── Auth (401 / 403) ──────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public Object handle(BadCredentialsException ex, HttpServletRequest req) {
        return isApi(req)
                ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Invalid email or password"))
                : errorView(401, "Invalid email or password");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handle(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied [{}]", req.getRequestURI());
        return isApi(req)
                ? ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("Access denied"))
                : errorView(403, "You don't have permission to access this page.");
    }

    // ── Catch-all (500) ───────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public Object handle(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error [{}]: {}", req.getRequestURI(), ex.getMessage(), ex);
        return isApi(req)
                ? ResponseEntity.internalServerError().body(new ErrorResponse("Something went wrong. Please try again."))
                : errorView(500, "Something went wrong. Please try again.");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isApi(HttpServletRequest req) {
        String accept = req.getHeader("Accept");
        return req.getRequestURI().startsWith("/api/") ||
               (accept != null && accept.contains("application/json"));
    }

    private ModelAndView errorView(int status, String message) {
        ModelAndView mv = new ModelAndView("error");
        mv.addObject("statusCode", status);
        mv.addObject("errorMessage", message);
        mv.setStatus(HttpStatus.valueOf(status));
        return mv;
    }

    public record ErrorResponse(String message) {}
}
