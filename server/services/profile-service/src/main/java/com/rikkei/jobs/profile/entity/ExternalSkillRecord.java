package com.rikkei.jobs.profile.entity;

import com.rikkei.jobs.profile.dto.VerifiedSkillRecord;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Kết quả từ hệ thống ngoài làm nguồn bằng chứng cho HS-01; chỉ kết quả đã chốt (verified) mới được tính.
 * NT-05 đồng bộ từ mock: {@code sourceKey} là khoá ổn định bên nguồn, {@code payload} giữ nguyên bản ghi gốc.
 */
@Entity
@Table(name = "external_records", uniqueConstraints = @UniqueConstraint(
        columnNames = {"user_id", "source", "source_ref"}))
@Getter
@Setter
@NoArgsConstructor
public class ExternalSkillRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MappingSource source;
    @Column(name = "source_code", nullable = false, length = 100)
    private String sourceCode;
    @Column(name = "source_ref", nullable = false)
    private String sourceRef;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, precision = 20, scale = 12)
    private BigDecimal score;
    @Column(name = "score_max", nullable = false, precision = 20, scale = 12)
    private BigDecimal scoreMax;
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
    @Column(nullable = false)
    private boolean verified;
    @Column(name = "source_key")
    private String sourceKey;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> payload;
    @Column(name = "synced_at")
    private Instant syncedAt;

    public VerifiedSkillRecord normalized() {
        if (!verified || score == null || scoreMax == null || scoreMax.signum() <= 0
                || score.signum() < 0 || score.compareTo(scoreMax) > 0) {
            throw new IllegalArgumentException("Only verified results with a valid score and scale can be imported");
        }
        return new VerifiedSkillRecord(source, sourceCode, sourceRef, title,
                score.multiply(BigDecimal.valueOf(100)).divide(scoreMax, 12, RoundingMode.HALF_UP), recordedAt);
    }

    @PrePersist
    @PreUpdate
    private void validatePrecision() {
        if (recordedAt == null || recordedAt.getNano() % 1_000 != 0
                || score == null || scoreMax == null || score.stripTrailingZeros().scale() > 12
                || scoreMax.stripTrailingZeros().scale() > 12) {
            throw new IllegalArgumentException("External results require an original microsecond timestamp and storable scores");
        }
    }
}
