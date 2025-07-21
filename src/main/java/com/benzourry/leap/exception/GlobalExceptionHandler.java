package com.benzourry.leap.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import com.networknt.schema.ValidationMessage;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JsonSchemaValidationException.class)
    public ResponseEntity<?> handleJsonSchemaValidation(JsonSchemaValidationException ex) {
        List<String> errorMessages = ex.getErrors().stream()
                .map(ValidationMessage::getMessage)
                .toList();

        return ResponseEntity.badRequest().body(Map.of(
                "message", "Data validation failed",
                "errors", errorMessages
        ));
    }
}
