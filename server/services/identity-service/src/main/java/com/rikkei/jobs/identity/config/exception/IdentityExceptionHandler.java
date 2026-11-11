package com.rikkei.jobs.identity.config.exception;

import com.rikkei.jobs.common.dto.response.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Bổ sung cho GlobalExceptionHandler của common-core: chỉ xử lý lỗi có mã riêng của identity. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        Object details = ex.getDetails().isEmpty() ? null : ex.getDetails();
        return ResponseEntity.status(ex.getCode().status())
                .body(ErrorResponse.of(ex.getCode().name(), ex.getMessage(), details));
    }
}
