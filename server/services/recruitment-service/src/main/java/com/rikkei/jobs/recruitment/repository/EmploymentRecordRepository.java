package com.rikkei.jobs.recruitment.repository;

import com.rikkei.jobs.recruitment.entity.EmploymentRecord;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmploymentRecordRepository extends JpaRepository<EmploymentRecord, UUID> {
    Optional<EmploymentRecord> findByApplicationId(UUID applicationId);
    List<EmploymentRecord> findByUserIdOrderByStartDateDesc(UUID userId);
}
