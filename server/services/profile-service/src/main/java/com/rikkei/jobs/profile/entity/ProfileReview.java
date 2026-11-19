package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "profile_reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProfileReview {
    public enum Action { SUBMIT, APPROVE, REJECT, WITHDRAW }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private Action action;
    private String reason;
    private UUID actorId;
    private Instant createdAt;

    public ProfileReview(UUID userId, Action action, String reason, UUID actorId, Instant createdAt) {
        this.userId = userId;
        this.action = action;
        this.reason = reason;
        this.actorId = actorId;
        this.createdAt = createdAt;
    }
}
