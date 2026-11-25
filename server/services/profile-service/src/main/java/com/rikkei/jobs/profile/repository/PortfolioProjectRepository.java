package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.PortfolioProject;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PortfolioProjectRepository extends JpaRepository<PortfolioProject, UUID> {
    List<PortfolioProject> findAllByUserIdOrderBySortOrderAscIdAsc(UUID userId);
    List<PortfolioProject> findAllByUserIdAndVisibleTrueOrderBySortOrderAscIdAsc(UUID userId);
    Optional<PortfolioProject> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndInternshipId(UUID userId, UUID internshipId);
    /** Khoá dòng dự án để hai lượt tải ảnh song song không vượt giới hạn số ảnh. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PortfolioProject p where p.id = :id and p.userId = :userId")
    Optional<PortfolioProject> lockByIdAndUserId(@Param("id") UUID id, @Param("userId") UUID userId);
    @Query("select coalesce(max(p.sortOrder), -1) from PortfolioProject p where p.userId = :userId")
    int maxSortOrder(@Param("userId") UUID userId);
}
