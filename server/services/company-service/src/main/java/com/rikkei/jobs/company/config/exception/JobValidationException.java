package com.rikkei.jobs.company.config.exception;

import java.util.Map;

/** Lỗi quy tắc nghiệp vụ của JD kèm chi tiết theo từng trường. */
public class JobValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public JobValidationException(Map<String, String> errors) {
        super("Dữ liệu không hợp lệ");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
