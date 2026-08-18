package com.autorentpro.identity.api;

import com.autorentpro.identity.application.PasswordChangeException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        basePackages = "com.autorentpro.identity.api"
)
public class IdentityApiExceptionHandler {

    @ExceptionHandler(
            PasswordChangeException.class
    )
    public ResponseEntity<SecurityErrorResponse>
    handlePasswordChangeException(
            PasswordChangeException exception,
            HttpServletRequest request
    ) {
        HttpStatus status =
                "ACCOUNT_UNAVAILABLE"
                        .equals(exception.getCode())
                        ? HttpStatus.FORBIDDEN
                        : HttpStatus.BAD_REQUEST;

        return ResponseEntity
                .status(status)
                .body(
                        SecurityErrorResponse.of(
                                status.value(),
                                exception.getCode(),
                                exception.getMessage(),
                                request.getRequestURI()
                        )
                );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    public ResponseEntity<SecurityErrorResponse>
    handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        HttpStatus status =
                HttpStatus.BAD_REQUEST;

        return ResponseEntity
                .status(status)
                .body(
                        SecurityErrorResponse.of(
                                status.value(),
                                "VALIDATION_FAILED",
                                "The request is invalid.",
                                request.getRequestURI()
                        )
                );
    }
}