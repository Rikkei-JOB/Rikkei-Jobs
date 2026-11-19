package com.rikkei.jobs.recruitment.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="interview_slots", schema="recruitment")
@Getter @Setter @NoArgsConstructor
public class InterviewSlot {
    @Id @Column(name="id") private UUID id;
    @Column(name="round_id") private UUID roundId;
    @Column(name="proposal_id") private UUID proposalId;
    @Column(name="start_at") private Instant startAt;
    @Column(name="end_at") private Instant endAt;
    @Column(name="status", length=16) private String status;
    @Column(name="created_at") private Instant createdAt;
}
