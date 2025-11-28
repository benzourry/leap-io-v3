package com.benzourry.leap.exception;

import com.networknt.schema.ValidationMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Map;

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

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxSizeException(MaxUploadSizeExceededException ex) {

//        Map<String, Object> body = new HashMap<>();
//        body.put("error", "File too large");
//        body.put("message", ex.getMessage());
//        body.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "error", "File too large",
                        "message", ex.getMessage(),
                        "status", HttpStatus.PAYLOAD_TOO_LARGE.value()
                ));
    }

//    @ExceptionHandler(UsernameNotFoundException.class)
//    public ResponseEntity<?> handleUsernameNotFoundException(UsernameNotFoundException ex) {
//
////        Map<String, Object> body = new HashMap<>();
////        body.put("error", "File too large");
////        body.put("message", ex.getMessage());
////        body.put("status", HttpStatus.PAYLOAD_TOO_LARGE.value());
//
//        return ResponseEntity
//                .status(HttpStatus.PAYLOAD_TOO_LARGE)
//                .contentType(MediaType.APPLICATION_JSON)
//                .body(Map.of(
//                        "error", "File too large",
//                        "message", ex.getMessage(),
//                        "status", HttpStatus.PAYLOAD_TOO_LARGE.value()
//                ));
//    }

}
