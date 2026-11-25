package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

/** HS-04: một bản CV theo vị trí. Chỉ giữ cấu hình, nội dung luôn đọc từ hồ sơ lúc xuất. */
@Entity @Table(name = "cvs")
@Getter @Setter @NoArgsConstructor
public class Cv {
    public static final String TEMPLATE_RIKKEI = "RIKKEI";
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false) private String name;
    @Column(name = "target_position") private String targetPosition;
    @Column(nullable = false, length = 32) private String template = TEMPLATE_RIKKEI;
    @Column(name = "public_token", nullable = false, length = 64, unique = true) private String publicToken;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
