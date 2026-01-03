package com.cource.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    // Keep default constructor
    public ConflictException() {
        super();
    }

    // Keep message constructor (Needed by your new Service logic)
    public ConflictException(String message) {
        super(message);
    }
}