package com.ranjeet.interview.exception;

import org.springframework.http.HttpStatus;

/** 400: a semantic input error that field annotations cannot express (from after to, unknown enum combination). */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
