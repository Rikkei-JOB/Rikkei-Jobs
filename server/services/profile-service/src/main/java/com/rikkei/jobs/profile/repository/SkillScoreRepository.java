package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.SkillScore;
import com.rikkei.jobs.profile.entity.SkillScoreId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillScoreRepository extends JpaRepository<SkillScore, SkillScoreId> {
    List<SkillScore> findAllByIdUserId(UUID userId);
}
