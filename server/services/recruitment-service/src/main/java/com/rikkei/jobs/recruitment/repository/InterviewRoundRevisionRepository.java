package com.rikkei.jobs.recruitment.repository;

import com.rikkei.jobs.recruitment.entity.InterviewRoundRevision;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRoundRevisionRepository extends JpaRepository<InterviewRoundRevision, UUID> {
    List<InterviewRoundRevision> findByApplicationIdOrderByCreatedAtAscIdAsc(UUID applicationId);
}
