package com.rikkei.jobs.recruitment.config;

import com.rikkei.jobs.common.dto.response.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.rikkei.jobs.recruitment")
public class RecruitmentDependencyExceptionHandler {
    @ExceptionHandler(RecruitmentDependencyException.class)
    public ResponseEntity<ErrorResponse> dependency(RecruitmentDependencyException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }
}
