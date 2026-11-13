package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "certificates")
@Getter @Setter @NoArgsConstructor
public class Certificate {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String issuer;
    @Column(name = "issued_at", nullable = false) private LocalDate issuedAt;
    @Column(name = "expires_at") private LocalDate expiresAt;
    @Column(name = "file_url", length = 1000) private String fileUrl;
    @Column(name = "file_type", length = 32) private String fileType;
}
