package com.rikkei.jobs.company.config.exception;

import com.rikkei.jobs.common.dto.response.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    @ExceptionHandler(JobValidationException.class)
    public ResponseEntity<ErrorResponse> handleJobValidation(JobValidationException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("VALIDATION_ERROR", "Dữ liệu không hợp lệ", ex.getErrors()));
    }

    /** Chỉ Job có @Version trong company-service: hai lần lưu JD chồng nhau → 409 CONFLICT. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONFLICT", "JD vừa được cập nhật ở nơi khác, vui lòng tải lại"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        CompanyErrorCode code = CompanyErrorCode.FILE_TOO_LARGE;
        return ResponseEntity.status(code.status())
                .body(ErrorResponse.of(code.name(), code.defaultMessage()));
    }
}
