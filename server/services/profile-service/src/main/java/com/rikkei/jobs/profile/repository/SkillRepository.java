package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    Optional<Skill> findByCode(String code);
}
