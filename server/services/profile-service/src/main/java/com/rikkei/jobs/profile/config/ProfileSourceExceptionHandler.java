package com.rikkei.jobs.profile.config;

import com.rikkei.jobs.common.dto.response.ErrorResponse;
import com.rikkei.jobs.profile.client.ProfileSourceException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.rikkei.jobs.profile")
public class ProfileSourceExceptionHandler {
    @ExceptionHandler(ProfileSourceException.class)
    public ResponseEntity<ErrorResponse> source(ProfileSourceException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }
}
