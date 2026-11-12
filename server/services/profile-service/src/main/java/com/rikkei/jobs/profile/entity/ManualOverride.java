package com.rikkei.jobs.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một lần cán bộ sửa tay một trường của external_records; đồng bộ sau đó không ghi đè trường này. */
@Entity
@Table(name = "manual_overrides")
@Getter
@Setter
@NoArgsConstructor
public class ManualOverride {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "record_id", nullable = false)
    private UUID recordId;
    @Convert(converter = OverrideFieldConverter.class)
    @Column(nullable = false, length = 30)
    private OverrideField field;
    @Column(name = "`value`", nullable = false)
    private String value;
    @Column(nullable = false, length = 500)
    private String reason;
    @Column(name = "staff_id", nullable = false)
    private UUID staffId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
