package com.rikkei.jobs.profile.config;

import com.rikkei.jobs.common.dto.response.ErrorResponse;
import com.rikkei.jobs.profile.service.ProfileFileException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Order(Ordered.HIGHEST_PRECEDENCE)
// Multipart parsing may fail before Spring selects a controller.
@RestControllerAdvice
public class ProfileFileExceptionHandler {
    @ExceptionHandler(ProfileFileException.class)
    public ResponseEntity<ErrorResponse> file(ProfileFileException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> tooLarge(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(413).body(ErrorResponse.of("FILE_TOO_LARGE", "Tệp vượt quá 5MB."));
    }
}
