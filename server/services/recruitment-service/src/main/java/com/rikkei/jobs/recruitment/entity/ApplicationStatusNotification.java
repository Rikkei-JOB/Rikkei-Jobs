package com.rikkei.jobs.recruitment.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "application_status_notification_outbox", schema = "recruitment")
@Getter @Setter @NoArgsConstructor
public class ApplicationStatusNotification {
    @Id private UUID id;
    @Column(name = "status_history_id", nullable = false, unique = true) private UUID statusHistoryId;
    @Column(name = "application_id", nullable = false) private UUID applicationId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, length = 20) private String status;
    @Column(nullable = false) private int attempts;
    @Column(name = "next_attempt_at", nullable = false) private Instant nextAttemptAt;
    @Column(name = "lease_until") private Instant leaseUntil;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "sent_at") private Instant sentAt;
}
