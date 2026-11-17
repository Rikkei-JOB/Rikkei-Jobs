package com.rikkei.jobs.profile.entity;

import java.util.Arrays;
import java.util.Optional;

/** Các trường của external_records cán bộ được sửa tay; giá trị lưu trong DB là {@link #key()}. */
public enum OverrideField {
    SCORE("score"), TITLE("title");

    private final String key;

    OverrideField(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Optional<OverrideField> fromKey(String key) {
        return Arrays.stream(values()).filter(f -> f.key.equals(key)).findFirst();
    }
}
