package com.notivio.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NotivioException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public NotivioException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public NotivioException(String message, HttpStatus status) {
        this(message, status, "NOTIVIO_ERROR");
    }
}
