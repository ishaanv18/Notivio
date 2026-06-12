package com.notivio.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends NotivioException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
