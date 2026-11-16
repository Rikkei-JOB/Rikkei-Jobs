package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.Education;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EducationRepository extends JpaRepository<Education, UUID> {
    List<Education> findAllByUserIdOrderByFromDateDescIdAsc(UUID userId);
    Optional<Education> findByIdAndUserId(UUID id, UUID userId);
}
