package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity @Table(name = "languages")
@Getter @Setter @NoArgsConstructor
public class Language {
    /** Khung tham chiếu châu Âu (CEFR), từ thấp đến cao. */
    public enum Level { A1, A2, B1, B2, C1, C2 }

    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 2) private Level level;
    private String note;
}
