package com.rikkei.jobs.recruitment.config;
import lombok.Getter;
@Getter
public class ContactException extends RuntimeException {
    private final int status;
    private final String code;
    public ContactException(int status,String code,String message){super(message);this.status=status;this.code=code;}
}
