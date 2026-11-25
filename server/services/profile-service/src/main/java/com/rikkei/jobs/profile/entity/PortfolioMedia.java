package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "portfolio_media")
@Getter @Setter @NoArgsConstructor
public class PortfolioMedia {
    public static final String IMAGE = "IMAGE";
    public static final String EVIDENCE = "EVIDENCE";
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "project_id", nullable = false) private UUID projectId;
    @Column(nullable = false, length = 16) private String type;
    /** IMAGE: đường dẫn tệp đã tải lên (/api/v1/files/{id}); EVIDENCE: link minh chứng từ IOC. */
    @Column(nullable = false, length = 1000) private String url;
    private String caption;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
