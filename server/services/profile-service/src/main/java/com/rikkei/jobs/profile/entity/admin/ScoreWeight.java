package com.rikkei.jobs.profile.entity.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Trọng số một nguồn điểm (DG-03 dùng khi gộp điểm). Tổng các nguồn bằng 100. */
@Entity
@Table(name = "score_weights")
@Getter
@Setter
@NoArgsConstructor
public class ScoreWeight {
    @Id
    @Column(length = 20)
    private String source;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
