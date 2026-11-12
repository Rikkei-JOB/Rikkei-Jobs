package com.rikkei.jobs.identity.util;

import java.security.SecureRandom;

/** Sinh mật khẩu tạm 12 ký tự cho tài khoản HR mới; luôn có chữ hoa, chữ thường và số (khớp {@link PasswordPolicy}). */
public final class TempPasswordGenerator {

    private static final int LENGTH = 12;
    // Bỏ các ký tự dễ nhầm (0/O, 1/l/I) vì HR phải gõ lại từ email.
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL = UPPER + LOWER + DIGITS;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TempPasswordGenerator() {
    }

    public static String generate() {
        char[] chars = new char[LENGTH];
        chars[0] = pick(UPPER);
        chars[1] = pick(LOWER);
        chars[2] = pick(DIGITS);
        for (int i = 3; i < LENGTH; i++) {
            chars[i] = pick(ALL);
        }
        // Xáo trộn (Fisher-Yates) để ba ký tự bắt buộc không luôn nằm đầu chuỗi.
        for (int i = LENGTH - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    private static char pick(String alphabet) {
        return alphabet.charAt(RANDOM.nextInt(alphabet.length()));
    }
}
