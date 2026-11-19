package com.rikkei.jobs.recruitment.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="interview_notification_outbox", schema="recruitment")
@Getter @Setter @NoArgsConstructor
public class InterviewNotificationOutbox {
    @Id @Column(name="id") private UUID id;
    @Column(name="event_key", length=100) private String eventKey;
    @Column(name="type", length=24) private String type;
    @Column(name="round_id") private UUID roundId;
    @Column(name="schedule_id") private UUID scheduleId;
    @Column(name="recipient", length=320) private String recipient;
    @Column(name="payload", columnDefinition="text") private String payload;
    @Column(name="status", length=16) private String status;
    @Column(name="attempts") private int attempts;
    @Column(name="next_attempt_at") private Instant nextAttemptAt;
    @Column(name="lease_until") private Instant leaseUntil;
    @Column(name="created_at") private Instant createdAt;
    @Column(name="sent_at") private Instant sentAt;
}
