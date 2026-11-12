package com.rikkei.jobs.profile.exception;

/** Lỗi dữ liệu đầu vào gắn với một trường; trả 400 VALIDATION_ERROR với {@code errors: {field: message}}. */
public class FieldValidationException extends RuntimeException {
    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
