package com.rikkei.jobs.profile.repository.admin;

import com.rikkei.jobs.profile.entity.admin.DataReport;
import com.rikkei.jobs.profile.entity.admin.DataReportStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataReportRepository extends JpaRepository<DataReport, UUID> {
    Page<DataReport> findAllByStatus(DataReportStatus status, Pageable pageable);

    List<DataReport> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByUserIdAndEvidenceIdAndStatus(UUID userId, UUID evidenceId, DataReportStatus status);
}
