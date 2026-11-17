package com.rikkei.jobs.recruitment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interview_rounds", schema = "recruitment")
@Getter @Setter @NoArgsConstructor
public class InterviewRound {
    @Id private UUID id;
    @Column(name = "application_id", nullable = false) private UUID applicationId;
    @Column(name = "round_no", nullable = false) private int roundNo;
    @Column(nullable = false, length = 255) private String name;
    @Column(name = "interviewer_email", nullable = false, length = 320) private String interviewerEmail;
    @Column(name = "scheduled_at") private Instant scheduledAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private InterviewRoundResult result;
    @Column(precision = 5, scale = 2) private BigDecimal score;
    @Column(length = 2000) private String note;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
