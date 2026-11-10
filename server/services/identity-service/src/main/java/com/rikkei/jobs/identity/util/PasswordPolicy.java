package com.rikkei.jobs.identity.util;

/** Quy tắc mật khẩu: tối thiểu 8 ký tự, có ít nhất một chữ và một số. */
public final class PasswordPolicy {

    public static final String MESSAGE = "Mật khẩu phải có ít nhất 8 ký tự, gồm cả chữ và số";
    public static final String REGEX = "^(?=.*\\p{L})(?=.*\\d).{8,}$";

    private PasswordPolicy() {
    }
}
