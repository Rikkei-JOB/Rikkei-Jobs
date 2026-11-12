package com.rikkei.jobs.company.config.exception;

import com.rikkei.jobs.common.dto.response.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/** Bổ sung cho GlobalExceptionHandler của common-core: chỉ xử lý lỗi có mã riêng của company-service. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CompanyExceptionHandler {

    @ExceptionHandler(CompanyException.class)
    public ResponseEntity<ErrorResponse> handleCompanyException(CompanyException ex) {
        return ResponseEntity.status(ex.getCode().status())
                .body(ErrorResponse.of(ex.getCode().name(), ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        CompanyErrorCode code = CompanyErrorCode.FILE_TOO_LARGE;
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code.name(), code.defaultMessage()));
    }
}
