package com.rikkei.jobs.recruitment.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name="application_status_history", schema="recruitment")
@Getter @Setter @NoArgsConstructor
public class ApplicationStatusHistory {
    @Id private UUID id;
    @Column(name="application_id", nullable=false) private UUID applicationId;
    @Enumerated(EnumType.STRING) @Column(name="from_status", length=20) private ApplicationStatus fromStatus;
    @Enumerated(EnumType.STRING) @Column(name="to_status", nullable=false, length=20) private ApplicationStatus toStatus;
    @Column(name="actor_id", nullable=false) private UUID actorId;
    @Column(length=1000) private String note;
    @Column(name="created_at", nullable=false) private Instant createdAt;
}
