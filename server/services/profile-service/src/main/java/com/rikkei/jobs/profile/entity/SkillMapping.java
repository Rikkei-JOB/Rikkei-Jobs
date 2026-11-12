package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "skill_mappings", uniqueConstraints = @UniqueConstraint(
        name = "uq_skill_mapping", columnNames = {"source", "source_code", "skill_id"}))
@Getter
@Setter
@NoArgsConstructor
public class SkillMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MappingSource source;

    @Column(name = "source_code", nullable = false, length = 100)
    private String sourceCode;

    @Column(name = "skill_id", nullable = false)
    private UUID skillId;

    @Column(nullable = false, precision = 20, scale = 12)
    private BigDecimal weight;
}
