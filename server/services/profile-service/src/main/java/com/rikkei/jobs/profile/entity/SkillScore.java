package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "skill_scores")
@Getter
@Setter
@NoArgsConstructor
public class SkillScore {
    @EmbeddedId
    private SkillScoreId id;

    @Column(name = "verified_score", precision = 20, scale = 6)
    private BigDecimal verifiedScore;

    @Column(name = "self_score", precision = 20, scale = 6)
    private BigDecimal selfScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SkillLevel level;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
