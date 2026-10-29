package com.ranjeet.interview.exception;

import org.springframework.http.HttpStatus;

/** 409: the request is well formed but the current state forbids it (insufficient balance, overlap, illegal transition, duplicate). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
