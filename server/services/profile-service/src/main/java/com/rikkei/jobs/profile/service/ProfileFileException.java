package com.rikkei.jobs.profile.service;

import lombok.Getter;

@Getter
public class ProfileFileException extends RuntimeException {
    private final int status;
    private final String code;
    public ProfileFileException(int status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
}
