package com.autorentpro.identity.api;

import com.autorentpro.identity.application.PasswordChangeException;
import com.autorentpro.identity.application.UserManagementException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

        return error(
                status,
                exception.getCode(),
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(
            UserManagementException.class
    )
    public ResponseEntity<SecurityErrorResponse>
    handleUserManagementException(
            UserManagementException exception,
            HttpServletRequest request
    ) {
        HttpStatus status =
                switch (exception.getCode()) {
                    case "USER_NOT_FOUND" ->
                            HttpStatus.NOT_FOUND;

                    case "EMAIL_ALREADY_IN_USE" ->
                            HttpStatus.CONFLICT;

                    default ->
                            HttpStatus.BAD_REQUEST;
                };

        return error(
                status,
                exception.getCode(),
                exception.getMessage(),
                request
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
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "The request is invalid.",
                request
        );
    }

    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<SecurityErrorResponse>
    handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "The request is invalid.",
                request
        );
    }

    private ResponseEntity<SecurityErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(status)
                .body(
                        SecurityErrorResponse.of(
                                status.value(),
                                code,
                                message,
                                request.getRequestURI()
                        )
                );
    }
}