package com.benzourry.leap.exception;

public class UpstreamServerErrorException extends RuntimeException {
    public UpstreamServerErrorException(String message) {
        super(message);
    }

    public UpstreamServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}