package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.MatchScore;
import com.rikkei.jobs.company.entity.MatchScoreId;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchScoreRepository extends JpaRepository<MatchScore, MatchScoreId> {

    /** Đánh dấu mọi điểm đệm của JD là cũ (gọi khi kỹ năng của JD đổi). */
    @Modifying(flushAutomatically = true)
    @Query("update MatchScore m set m.stale = true where m.jobId = :jobId")
    int markStaleByJobId(@Param("jobId") UUID jobId);
}
