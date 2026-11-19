package com.rikkei.jobs.profile.entity.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ứng viên báo một bằng chứng sai; ADMIN xử lý (RESOLVED/REJECTED) kèm kết quả. */
@Entity
@Table(name = "data_reports")
@Getter
@Setter
@NoArgsConstructor
public class DataReport {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Null khi bằng chứng đã bị xoá (ví dụ nguồn ngoài không còn bản ghi); tên giữ ở evidenceTitle. */
    @Column(name = "evidence_id")
    private UUID evidenceId;

    @Column(name = "evidence_title", nullable = false)
    private String evidenceTitle;

    @Column(name = "skill_name", nullable = false)
    private String skillName;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DataReportStatus status = DataReportStatus.OPEN;

    @Column(length = 2000)
    private String resolution;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
