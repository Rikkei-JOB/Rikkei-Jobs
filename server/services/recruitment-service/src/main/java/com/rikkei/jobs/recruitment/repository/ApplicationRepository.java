package com.rikkei.jobs.recruitment.repository;

import com.rikkei.jobs.recruitment.entity.*;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    Page<Application> findByUserId(UUID userId, Pageable page);
    Page<Application> findByUserIdAndStatus(UUID userId, ApplicationStatus status, Pageable page);
    boolean existsByUserIdAndJobIdAndStatusNotIn(UUID userId, UUID jobId, Collection<ApplicationStatus> statuses);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id")
    Optional<Application> findForUpdate(@Param("id") UUID id);
}
