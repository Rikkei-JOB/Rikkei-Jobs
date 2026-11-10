package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.SkillMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SkillMappingRepository extends JpaRepository<SkillMapping, UUID> {
}
