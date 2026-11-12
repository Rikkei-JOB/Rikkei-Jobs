package com.rikkei.jobs.common.config.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.rikkei.jobs.common.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.putIfAbsent(fieldName, errorMessage);
        });

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", "Dữ liệu không hợp lệ", errors));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("BAD_REQUEST", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Tham số '%s' có giá trị '%s' không đúng định dạng.", ex.getName(), ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("BAD_REQUEST", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String message = "Dữ liệu yêu cầu không đúng định dạng hoặc giá trị không hợp lệ.";
        if (ex.getCause() instanceof InvalidFormatException ife && ife.getTargetType().isEnum()) {
            List<String> acceptedValues = stream(ife.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.toList());
            message = String.format("Giá trị '%s' không hợp lệ. Chỉ chấp nhận các giá trị: %s",
                    ife.getValue(), String.join(", ", acceptedValues));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("BAD_REQUEST", message));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONFLICT", ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("Vi phạm ràng buộc dữ liệu: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CONFLICT", "Dữ liệu vi phạm ràng buộc"));
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleForbiddenException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of("FORBIDDEN", ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("NOT_FOUND", "Không tìm thấy tài nguyên"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of("METHOD_NOT_ALLOWED", "Phương thức không được hỗ trợ"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of("UNSUPPORTED_MEDIA_TYPE", "Định dạng dữ liệu không được hỗ trợ"));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPartException(MissingServletRequestPartException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of("VALIDATION_ERROR", "Dữ liệu không hợp lệ",
                        Map.of(ex.getRequestPartName(), "Thiếu tệp tải lên")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) throws Exception {
        // Các lỗi Spring MVC khác (ngoài 404, 405, 415 đã xử lý ở trên) giữ nguyên cách xử lý mặc định.
        if (ex instanceof org.springframework.web.ErrorResponse) {
            throw ex;
        }
        log.error("Lỗi hệ thống không xác định", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Lỗi hệ thống"));
    }
}
