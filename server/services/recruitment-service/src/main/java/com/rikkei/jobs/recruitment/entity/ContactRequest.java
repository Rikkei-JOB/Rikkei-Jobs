package com.rikkei.jobs.recruitment.entity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
@Entity @Table(name="contact_requests", schema="recruitment")
@Getter @Setter @NoArgsConstructor
public class ContactRequest {
    @Id private UUID id;
    private UUID companyId;
    private UUID userId;
    private UUID jobId;
    @Column(length=2000) private String reason;
    @Enumerated(EnumType.STRING) private ContactRequestStatus status;
    private UUID createdBy;
    private Instant createdAt;
    private UUID reviewedBy;
    private Instant reviewedAt;
    @Column(length=2000) private String rejectionReason;
}
