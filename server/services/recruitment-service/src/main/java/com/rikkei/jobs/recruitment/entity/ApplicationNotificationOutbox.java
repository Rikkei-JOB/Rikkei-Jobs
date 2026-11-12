package com.rikkei.jobs.recruitment.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name="application_notification_outbox", schema="recruitment")
@Getter @Setter @NoArgsConstructor
public class ApplicationNotificationOutbox {
    @Id private UUID id;
    @Column(name="application_id", nullable=false) private UUID applicationId;
    @Column(nullable=false, length=320) private String recipient;
    @Column(nullable=false, length=20) private String status;
    @Column(nullable=false) private int attempts;
    @Column(name="next_attempt_at", nullable=false) private Instant nextAttemptAt;
    @Column(name="lease_until") private Instant leaseUntil;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="sent_at") private Instant sentAt;
}
