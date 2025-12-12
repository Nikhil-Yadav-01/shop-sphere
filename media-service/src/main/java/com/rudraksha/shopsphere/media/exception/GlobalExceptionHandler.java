package com.rudraksha.shopsphere.media.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MediaStorageException.class)
    public ResponseEntity<Map<String, Object>> handleStorageException(MediaStorageException ex) {
        Map<String, Object> body = Map.of(
                "status", "error",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MediaNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(MediaNotFoundException ex) {
        Map<String, Object> body = Map.of(
                "status", "error",
                "message", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        Map<String, Object> body = Map.of(
                "status", "error",
                "message", "Internal server error",
                "detail", ex.getMessage(),
                "timestamp", LocalDateTime.now().toString()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
