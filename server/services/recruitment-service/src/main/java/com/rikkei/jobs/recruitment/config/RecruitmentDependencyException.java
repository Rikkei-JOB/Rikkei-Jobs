package com.rikkei.jobs.recruitment.config;

import lombok.Getter;

@Getter
public class RecruitmentDependencyException extends RuntimeException {
    private final int status;
    private final String code;

    public RecruitmentDependencyException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}
