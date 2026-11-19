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

/** Một trường bị ADMIN sửa: giá trị cũ, mới, lý do và người sửa. Chỉ thêm, không sửa, không xoá. */
@Entity
@Table(name = "change_logs")
@Getter
@Setter
@NoArgsConstructor
public class ChangeLog {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ChangeEntity entity;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @Column(nullable = false, length = 100)
    private String field;

    @Column(name = "old_value", length = 5000)
    private String oldValue;

    @Column(name = "new_value", length = 5000)
    private String newValue;

    @Column(length = 1000)
    private String reason;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
