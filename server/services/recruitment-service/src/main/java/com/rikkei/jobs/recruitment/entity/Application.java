package com.rikkei.jobs.recruitment.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "applications", schema = "recruitment")
@Getter @Setter @NoArgsConstructor
public class Application {
    @Id private UUID id;
    @Column(name="job_id", nullable=false) private UUID jobId;
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="company_id", nullable=false) private UUID companyId;
    @Column(name="job_title", nullable=false, length=500) private String jobTitle;
    @Column(name="company_name", nullable=false, length=500) private String companyName;
    @Column(name="cv_id", nullable=false) private UUID cvId;
    @Column(name="cv_snapshot_url", nullable=false, length=1000) private String cvSnapshotUrl;
    @Column(name="cover_letter", length=5000) private String coverLetter;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private ApplicationStatus status;
    @Column(name="created_at", nullable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
}
