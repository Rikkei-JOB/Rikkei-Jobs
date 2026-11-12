package com.rikkei.jobs.company.config.exception;

import org.springframework.http.HttpStatus;

/** Mã lỗi riêng của company-service (ngoài các mã chung của common-core). */
public enum CompanyErrorCode {
    COMPANY_LOCKED(HttpStatus.FORBIDDEN, "Công ty đang bị khoá"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "Ảnh vượt quá 2MB"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "Chỉ chấp nhận ảnh JPG hoặc PNG"),
    EXTERNAL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Không kết nối được dịch vụ tài khoản, vui lòng thử lại");

    private final HttpStatus status;
    private final String defaultMessage;

    CompanyErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
