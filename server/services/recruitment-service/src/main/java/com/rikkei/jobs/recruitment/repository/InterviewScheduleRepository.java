package com.rikkei.jobs.recruitment.repository;
import com.rikkei.jobs.recruitment.entity.InterviewSchedule;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InterviewScheduleRepository extends JpaRepository<InterviewSchedule,UUID> {
    List<InterviewSchedule> findByRoundIdOrderByCreatedAtDesc(UUID roundId);
    Optional<InterviewSchedule> findByRoundIdAndStatus(UUID roundId,String status);
}
