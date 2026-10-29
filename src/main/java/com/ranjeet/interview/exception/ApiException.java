package com.ranjeet.interview.exception;

import org.springframework.http.HttpStatus;

/**
 * Base of every exception that maps to an HTTP status. GlobalExceptionHandler turns any subclass
 * into the standard ErrorResponse, so a new failure mode is one small class and no handler change.
 * For a one-off status, {@code new ApiException(HttpStatus.GONE, "...")} works as is.
 * The message is client-facing: say what is wrong, never leak internals.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        this(status, message, null);
    }

    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
