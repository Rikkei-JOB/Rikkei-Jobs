package com.rikkei.jobs.identity.config.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INVALID_OLD_PASSWORD(HttpStatus.BAD_REQUEST, "Mật khẩu cũ không đúng"),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải khác mật khẩu cũ"),
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"),
    INVALID_TICKET(HttpStatus.UNAUTHORIZED, "Vé SSO không hợp lệ"),
    TICKET_EXPIRED(HttpStatus.UNAUTHORIZED, "Vé SSO đã hết hạn"),
    TICKET_ALREADY_USED(HttpStatus.UNAUTHORIZED, "Vé SSO đã được sử dụng"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc phiên đã hết hạn"),

    MUST_CHANGE_PASSWORD(HttpStatus.FORBIDDEN, "Bạn phải đổi mật khẩu trước khi tiếp tục"),
    ACCOUNT_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "Tài khoản đang bị khoá"),

    LMS_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "Không kết nối được LMS");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
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
