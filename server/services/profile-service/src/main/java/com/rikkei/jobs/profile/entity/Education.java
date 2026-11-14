package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "educations")
@Getter @Setter @NoArgsConstructor
public class Education {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private String school;
    private String major;
    @Column(name = "from_date", nullable = false) private LocalDate fromDate;
    @Column(name = "to_date") private LocalDate toDate;
    @Column(precision = 10, scale = 4) private BigDecimal gpa;
    @Column(length = 5000) private String note;
}
