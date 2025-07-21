package com.benzourry.leap.exception;

import com.networknt.schema.ValidationMessage;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Set;
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class JsonSchemaValidationException extends RuntimeException {
    private final Set<ValidationMessage> errors;

    public JsonSchemaValidationException(Set<ValidationMessage> errors) {
        super("Data validation failed");
        this.errors = errors;
    }

    public Set<ValidationMessage> getErrors() {
        return errors;
    }
}