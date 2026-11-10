package com.rikkei.jobs.profile.dto;

import com.rikkei.jobs.profile.entity.MappingSource;

import java.math.BigDecimal;
import java.time.Instant;

/** Internal input for final, verified records; the NT-05 adapter will establish trust and normalize scores. */
public record VerifiedSkillRecord(MappingSource source, String sourceCode, String sourceRef,
                                  String title, BigDecimal score, Instant recordedAt) {
    public VerifiedSkillRecord {
        if (source == null || sourceCode == null || sourceCode.isBlank() || sourceCode.length() > 100
                || sourceRef == null || sourceRef.isBlank() || sourceRef.length() > 255
                || title == null || title.isBlank() || title.length() > 255 || recordedAt == null
                || recordedAt.getNano() % 1_000 != 0
                || score == null || score.signum() < 0 || score.compareTo(new BigDecimal("100")) > 0
                || score.stripTrailingZeros().scale() > 12) {
            throw new IllegalArgumentException("A verified result needs its source, code, reference, title, original date (microsecond precision) and score 0–100 (up to 12 decimals)");
        }
    }
}
