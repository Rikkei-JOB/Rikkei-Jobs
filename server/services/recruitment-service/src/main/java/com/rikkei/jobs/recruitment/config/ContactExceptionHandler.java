package com.rikkei.jobs.recruitment.config;
import com.rikkei.jobs.common.dto.response.ErrorResponse;
import org.springframework.core.*;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataIntegrityViolationException;
@Order(Ordered.HIGHEST_PRECEDENCE) @RestControllerAdvice(basePackages="com.rikkei.jobs.recruitment")
public class ContactExceptionHandler {
    @ExceptionHandler(ContactException.class)
    public ResponseEntity<ErrorResponse> contact(ContactException e){return ResponseEntity.status(e.getStatus()).body(ErrorResponse.of(e.getCode(),e.getMessage()));}
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e) {
        for(Throwable t=e;t!=null;t=t.getCause()) {
            if(t instanceof org.hibernate.exception.ConstraintViolationException v && "uq_contact_request_pending".equals(v.getConstraintName()))
                return ResponseEntity.status(409).body(ErrorResponse.of("CONTACT_REQUEST_PENDING","Yêu cầu đang chờ duyệt."));
        }
        return ResponseEntity.status(500).body(ErrorResponse.of("INTERNAL_ERROR","Không thể lưu dữ liệu."));
    }
}
