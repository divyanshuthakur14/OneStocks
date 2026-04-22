package com.onestocks.app.exception;

import com.onestocks.app.dto.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity.badRequest()
                .body(AuthResponse.builder().success(false).message(errors).build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponse> handleGenericError(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(AuthResponse.builder().success(false).message("Something went wrong: " + ex.getMessage()).build());
    }
}