package com.rikkei.jobs.profile.dto;

import com.rikkei.jobs.profile.entity.MappingSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerifiedSkillRecordTest {
    @ParameterizedTest
    @ValueSource(strings = {"-0.1", "100.01", "0.0000000000001"})
    void rejectsScoresThatCannotBeStoredWithoutChangingTheirMeaning(String score) {
        assertThatThrownBy(() -> new VerifiedSkillRecord(MappingSource.LMS, "JAVA01", "result-1",
                "Kết quả Java", new BigDecimal(score), Instant.parse("2026-10-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMissingReferenceAndOriginalDate() {
        assertThatThrownBy(() -> new VerifiedSkillRecord(MappingSource.LMS, "JAVA01", " ",
                "Kết quả Java", BigDecimal.TEN, Instant.parse("2026-10-01T00:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new VerifiedSkillRecord(MappingSource.LMS, "JAVA01", "result-1",
                "Kết quả Java", BigDecimal.TEN, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDatePrecisionThatWouldChangeAfterDatabaseReload() {
        assertThatThrownBy(() -> new VerifiedSkillRecord(MappingSource.EXAM, "JAVA01", "near-cutoff",
                "Thi Java", new BigDecimal("100"), Instant.parse("2025-11-10T23:59:59.999999999Z")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
