package com.rikkei.jobs.recruitment.repository;

import com.rikkei.jobs.recruitment.entity.ApplicationStatusHistory;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {
    List<ApplicationStatusHistory> findByApplicationIdOrderByCreatedAtAscIdAsc(UUID applicationId);
}
