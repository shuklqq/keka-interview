package com.ranjeet.interview.exception;

import org.springframework.http.HttpStatus;

/** 403: the caller is known but not allowed to do this (not the owner, not the manager). */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
