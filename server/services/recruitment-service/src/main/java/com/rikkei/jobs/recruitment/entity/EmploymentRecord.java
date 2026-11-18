package com.rikkei.jobs.recruitment.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "employment_records", schema = "recruitment")
@Getter @Setter @NoArgsConstructor
public class EmploymentRecord {
    @Id private UUID id;
    @Column(name = "application_id", nullable = false, unique = true) private UUID applicationId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "company_id", nullable = false) private UUID companyId;
    @Column(name = "job_id", nullable = false) private UUID jobId;
    @Column(nullable = false, length = 255) private String position;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "confirmed_at") private Instant confirmedAt;
}
