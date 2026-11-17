package com.rikkei.jobs.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Nhật ký một lần đồng bộ NT-05. */
@Entity
@Table(name = "sync_runs")
@Getter
@Setter
@NoArgsConstructor
public class SyncRun {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger", nullable = false, length = 10)
    private SyncTrigger trigger;
    @Column(name = "user_id")
    private UUID userId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SyncStatus status;
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;
    @Column(name = "finished_at", nullable = false)
    private Instant finishedAt;
    @Column(length = 1000)
    private String error;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Integer> counts;
    @Column(name = "triggered_by")
    private UUID triggeredBy;
}
