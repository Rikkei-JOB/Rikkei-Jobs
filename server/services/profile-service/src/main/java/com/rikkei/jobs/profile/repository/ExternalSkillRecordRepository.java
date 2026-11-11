package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.ExternalSkillRecord;
import com.rikkei.jobs.profile.entity.MappingSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExternalSkillRecordRepository extends JpaRepository<ExternalSkillRecord, UUID> {
    List<ExternalSkillRecord> findAllByUserIdAndVerifiedTrue(UUID userId);
    Optional<ExternalSkillRecord> findByUserIdAndSourceAndSourceRef(UUID userId, MappingSource source, String sourceRef);
}
