package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.PortfolioMedia;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioMediaRepository extends JpaRepository<PortfolioMedia, UUID> {
    List<PortfolioMedia> findAllByProjectIdInOrderByCreatedAtAscIdAsc(Collection<UUID> projectIds);
    long countByProjectIdAndType(UUID projectId, String type);
}
