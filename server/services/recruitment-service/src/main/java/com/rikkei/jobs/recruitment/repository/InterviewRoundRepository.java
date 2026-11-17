package com.rikkei.jobs.recruitment.repository;

import com.rikkei.jobs.recruitment.entity.InterviewRound;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, UUID> {
    List<InterviewRound> findByApplicationIdOrderByRoundNoAsc(UUID applicationId);
    Optional<InterviewRound> findByIdAndApplicationId(UUID id, UUID applicationId);
    @Query("select coalesce(max(r.roundNo), 0) from InterviewRound r where r.applicationId = :applicationId")
    int maxRoundNo(@Param("applicationId") UUID applicationId);
}
