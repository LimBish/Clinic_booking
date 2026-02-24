package com.clinic.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String entity, Object id) {
        super(entity + " not found with id: " + id);
    }
    public NotFoundException(String message) {
        super(message);
    }
}
