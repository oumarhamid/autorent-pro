package com.autorentpro.agency.api;

import com.autorentpro.agency.application.AgencyManagementException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        basePackages = "com.autorentpro.agency.api"
)
public class AgencyApiExceptionHandler {

    @ExceptionHandler(
            AgencyManagementException.class
    )
    public ResponseEntity<AgencyErrorResponse>
    handleAgencyManagementException(
            AgencyManagementException exception,
            HttpServletRequest request
    ) {
        HttpStatus status =
                switch (exception.getCode()) {
                    case "AGENCY_NOT_FOUND" ->
                            HttpStatus.NOT_FOUND;

                    case "AGENCY_CODE_ALREADY_IN_USE" ->
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
    public ResponseEntity<AgencyErrorResponse>
    handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        return validationError(
                request
        );
    }

    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    public ResponseEntity<AgencyErrorResponse>
    handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return validationError(
                request
        );
    }

    @ExceptionHandler(
            IllegalArgumentException.class
    )
    public ResponseEntity<AgencyErrorResponse>
    handleDomainValidationException(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "The request is invalid.",
                request
        );
    }

    private ResponseEntity<AgencyErrorResponse>
    validationError(
            HttpServletRequest request
    ) {
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "The request is invalid.",
                request
        );
    }

    private ResponseEntity<AgencyErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(status)
                .body(
                        AgencyErrorResponse.of(
                                status.value(),
                                code,
                                message,
                                request.getRequestURI()
                        )
                );
    }
}