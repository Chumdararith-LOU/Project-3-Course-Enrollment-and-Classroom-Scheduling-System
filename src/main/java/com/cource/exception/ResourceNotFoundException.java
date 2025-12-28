package com.cource.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException() {
        super();
    }

    // Fix: Add constructor accepting a message string
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
