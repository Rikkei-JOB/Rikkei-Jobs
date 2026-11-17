package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<Job> findByCompanyId(UUID companyId, Pageable pageable);

    Page<Job> findByCompanyIdAndStatus(UUID companyId, JobStatus status, Pageable pageable);

    /** Mỗi dòng là [companyId, số JD]; công ty không có JD phù hợp sẽ không xuất hiện. */
    @Query("select j.companyId, count(j) from Job j where j.companyId in :ids and j.status <> :excluded group by j.companyId")
    List<Object[]> countByCompanyIdsExcludingStatus(@Param("ids") Collection<UUID> ids,
                                                    @Param("excluded") JobStatus excluded);
}
