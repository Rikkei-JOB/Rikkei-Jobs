package com.rikkei.jobs.identity.config.exception;

import lombok.Getter;

import java.util.Map;

/** Lỗi nghiệp vụ của identity có mã lỗi riêng (ví dụ TICKET_EXPIRED, ACCOUNT_LOCKED) mà FE cần phân biệt. */
@Getter
public class ApiException extends RuntimeException {
    private final ErrorCode code;
    private final transient Map<String, Object> details;

    public ApiException(ErrorCode code) {
        this(code, code.defaultMessage(), Map.of());
    }

    public ApiException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public ApiException(ErrorCode code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details;
    }
}
