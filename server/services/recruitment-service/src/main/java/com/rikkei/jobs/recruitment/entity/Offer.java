package com.rikkei.jobs.recruitment.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "offers", schema = "recruitment")
@Getter @Setter @NoArgsConstructor
public class Offer {
    @Id private UUID id;
    @Column(name = "application_id", nullable = false, unique = true) private UUID applicationId;
    @Column(nullable = false, length = 255) private String position;
    @Column(nullable = false) private Long salary;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "work_location", nullable = false, length = 500) private String workLocation;
    @Column(length = 2000) private String note;
    @Column(name = "response_deadline", nullable = false) private Instant responseDeadline;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OfferStatus status;
    @Column(name = "responded_at") private Instant respondedAt;
    @Column(name = "decline_reason", length = 1000) private String declineReason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
