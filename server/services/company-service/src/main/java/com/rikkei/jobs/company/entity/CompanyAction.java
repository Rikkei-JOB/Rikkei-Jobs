package com.rikkei.jobs.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Một dòng lịch sử thao tác của cán bộ trên công ty. Chỉ thêm, không sửa, không xoá. */
@Entity
@Table(name = "company_actions")
@Getter
@NoArgsConstructor
public class CompanyAction {

    @Id
    private UUID id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private UUID companyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 12)
    private CompanyActionType action;

    /** Id JD với các thao tác *_JD; null với LOCK/UNLOCK. */
    @Column(name = "target_id", updatable = false)
    private UUID targetId;

    @Column(updatable = false, length = 1000)
    private String reason;

    @Column(name = "staff_id", nullable = false, updatable = false)
    private UUID staffId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public CompanyAction(UUID companyId, CompanyActionType action, UUID targetId, String reason, UUID staffId) {
        this.companyId = companyId;
        this.action = action;
        this.targetId = targetId;
        this.reason = reason;
        this.staffId = staffId;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }
}
