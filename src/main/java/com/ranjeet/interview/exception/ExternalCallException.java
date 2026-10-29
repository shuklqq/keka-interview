package com.ranjeet.interview.exception;

import org.springframework.http.HttpStatus;

/** 502: an outbound HTTP call failed (non-2xx, timeout, connection refused). */
public class ExternalCallException extends ApiException {

    public ExternalCallException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }
}
