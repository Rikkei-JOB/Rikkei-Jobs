package com.rikkei.jobs.company.config.exception;

public class CompanyException extends RuntimeException {

    private final CompanyErrorCode code;

    public CompanyException(CompanyErrorCode code) {
        super(code.defaultMessage());
        this.code = code;
    }

    public CompanyErrorCode getCode() {
        return code;
    }
}
