package com.rikkei.jobs.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Bảng đệm điểm khớp giữa một JD và một ứng viên; detail là JSON gồm dữ liệu vào và kết quả. */
@Entity
@Table(name = "match_scores")
@IdClass(MatchScoreId.class)
@Getter
@Setter
@NoArgsConstructor
public class MatchScore {

    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detail;

    @Column(nullable = false)
    private boolean stale;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    public MatchScore(UUID jobId, UUID userId, int score, String detail, boolean stale, Instant computedAt) {
        this.jobId = jobId;
        this.userId = userId;
        this.score = score;
        this.detail = detail;
        this.stale = stale;
        this.computedAt = computedAt;
    }
}
