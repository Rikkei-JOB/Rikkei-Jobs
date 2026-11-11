package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "skill_evidences", uniqueConstraints = @UniqueConstraint(
        name = "uq_skill_evidence", columnNames = {"user_id", "skill_id", "source", "source_ref"}))
@Getter
@Setter
@NoArgsConstructor
public class SkillEvidence {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(name = "external_record_id")
    private UUID externalRecordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EvidenceSource source;

    @Column(name = "source_ref", nullable = false)
    private String sourceRef;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, precision = 20, scale = 12)
    private BigDecimal score;

    // Keep the applied mapping weight so later recomputes use the same evidence input.
    @Column(nullable = false, precision = 20, scale = 12)
    private BigDecimal weight;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(nullable = false)
    private boolean verified;
}
