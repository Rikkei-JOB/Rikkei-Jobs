package com.rikkei.jobs.recruitment.config;

import com.rikkei.jobs.common.dto.response.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.rikkei.jobs.recruitment")
public class InvalidTransitionExceptionHandler {
    @ExceptionHandler(InvalidTransitionException.class)
    public ResponseEntity<ErrorResponse> invalid(InvalidTransitionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse.of("INVALID_TRANSITION", ex.getMessage()));
    }
}
