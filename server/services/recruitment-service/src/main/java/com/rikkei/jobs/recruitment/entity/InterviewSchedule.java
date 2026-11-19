package com.rikkei.jobs.recruitment.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="interview_schedules", schema="recruitment")
@Getter @Setter @NoArgsConstructor
public class InterviewSchedule {
    @Id @Column(name="id") private UUID id;
    @Column(name="round_id") private UUID roundId;
    @Column(name="slot_id") private UUID slotId;
    @Column(name="meet_url", length=2000) private String meetUrl;
    @Column(name="status", length=16) private String status;
    @Column(name="candidate_id") private UUID candidateId;
    @Column(name="candidate_email", length=320) private String candidateEmail;
    @Column(name="interviewer_email", length=320) private String interviewerEmail;
    @Column(name="reminded_24h_at") private Instant reminded24hAt;
    @Column(name="reminded_1h_at") private Instant reminded1hAt;
    @Column(name="reminder_1h_suppressed") private boolean reminder1hSuppressed;
    @Column(name="created_at") private Instant createdAt;
    @Column(name="cancelled_at") private Instant cancelledAt;
}
