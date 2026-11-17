package com.rikkei.jobs.recruitment.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "interview_round_revisions", schema = "recruitment")
@Getter @Setter @NoArgsConstructor
public class InterviewRoundRevision {
    @Id private UUID id;
    @Column(name = "round_id", nullable = false) private UUID roundId;
    @Column(name = "application_id", nullable = false) private UUID applicationId;
    @Column(name = "actor_id", nullable = false) private UUID actorId;
    @Enumerated(EnumType.STRING) @Column(name = "old_result", nullable = false, length = 10) private InterviewRoundResult oldResult;
    @Enumerated(EnumType.STRING) @Column(name = "new_result", nullable = false, length = 10) private InterviewRoundResult newResult;
    @Column(name = "old_score", precision = 5, scale = 2) private BigDecimal oldScore;
    @Column(name = "new_score", precision = 5, scale = 2) private BigDecimal newScore;
    @Column(name = "old_note", length = 2000) private String oldNote;
    @Column(name = "new_note", length = 2000) private String newNote;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
