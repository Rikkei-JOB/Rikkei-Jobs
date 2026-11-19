package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.SkillEvidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillEvidenceRepository extends JpaRepository<SkillEvidence, UUID> {
    List<SkillEvidence> findAllByUserId(UUID userId);

    boolean existsBySkillId(UUID skillId);

    @org.springframework.data.jpa.repository.Query("select distinct e.skillId from SkillEvidence e")
    List<UUID> findUsedSkillIds();
}
