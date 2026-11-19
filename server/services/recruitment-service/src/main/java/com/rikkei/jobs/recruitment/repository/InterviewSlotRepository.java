package com.rikkei.jobs.recruitment.repository;
import com.rikkei.jobs.recruitment.entity.InterviewSlot;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InterviewSlotRepository extends JpaRepository<InterviewSlot,UUID> {
    List<InterviewSlot> findByRoundIdOrderByStartAtAsc(UUID roundId);
}
