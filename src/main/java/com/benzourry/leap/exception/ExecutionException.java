package com.benzourry.leap.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ExecutionException extends RuntimeException {

    public ExecutionException(String message) {
        super(message);
    }
}
