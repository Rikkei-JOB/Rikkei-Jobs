package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.SyncRun;
import com.rikkei.jobs.profile.entity.SyncStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncRunRepository extends JpaRepository<SyncRun, UUID> {
    @Query("select r from SyncRun r where (:status is null or r.status = :status) "
            + "and (:userId is null or r.userId = :userId)")
    Page<SyncRun> search(@Param("status") SyncStatus status, @Param("userId") UUID userId, Pageable pageable);

    Optional<SyncRun> findFirstByUserIdOrderByStartedAtDesc(UUID userId);

    /** Lần chạy mới nhất của từng ứng viên trong danh sách. */
    @Query("select r from SyncRun r where r.userId in :userIds and r.startedAt = "
            + "(select max(o.startedAt) from SyncRun o where o.userId = r.userId)")
    List<SyncRun> findLatestByUserIds(@Param("userIds") Collection<UUID> userIds);
}
