package com.rikkei.jobs.company.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Kỹ năng yêu cầu của một JD; skill_id là id danh mục của profile-service, không có FK chéo schema. */
@Entity
@Table(name = "job_skills")
@IdClass(JobSkillId.class)
@Getter
@Setter
@NoArgsConstructor
public class JobSkill {

    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Id
    @Column(name = "skill_id")
    private UUID skillId;

    @Column(nullable = false)
    private boolean required;

    @Column(name = "min_score", nullable = false)
    private int minScore;

    public JobSkill(UUID jobId, UUID skillId, boolean required, int minScore) {
        this.jobId = jobId;
        this.skillId = skillId;
        this.required = required;
        this.minScore = minScore;
    }
}
