package com.rikkei.jobs.company.util;

import com.rikkei.jobs.company.config.exception.JobValidationException;
import java.util.Map;

/** Lý do của cán bộ (trả lại / đóng JD, khoá công ty): bắt buộc, tính sau khi strip, 1–1000 ký tự. */
public final class ReasonRules {

    public static final int MAX_LENGTH = 1000;
    public static final String REQUIRED_MESSAGE = "Vui lòng nhập lý do";
    public static final String TOO_LONG_MESSAGE = "Lý do tối đa 1000 ký tự";

    private ReasonRules() {
    }

    /**
     * Trả lý do đã strip. {@code @NotBlank} chỉ bỏ khoảng trắng ASCII và {@code @Size} đếm cả khoảng trắng hai đầu,
     * nên độ dài được kiểm ở đây trên chuỗi đã strip; sai → 400 VALIDATION_ERROR với {@code errors.reason}.
     */
    public static String clean(String reason) {
        String cleaned = reason == null ? "" : reason.strip();
        // strip() không bỏ khoảng trắng không ngắt (U+00A0, U+2007, U+202F): lý do chỉ gồm các ký tự đó cũng là rỗng.
        if (cleaned.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new JobValidationException(Map.of("reason", REQUIRED_MESSAGE));
        }
        if (cleaned.length() > MAX_LENGTH) {
            throw new JobValidationException(Map.of("reason", TOO_LONG_MESSAGE));
        }
        return cleaned;
    }
}
