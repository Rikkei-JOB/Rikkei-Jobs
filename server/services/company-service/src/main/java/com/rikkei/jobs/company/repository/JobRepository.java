package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

    Optional<Job> findByIdAndCompanyId(UUID id, UUID companyId);

    Page<Job> findByCompanyId(UUID companyId, Pageable pageable);

    Page<Job> findByCompanyIdAndStatus(UUID companyId, JobStatus status, Pageable pageable);

    /** Mỗi dòng là [companyId, số JD]; công ty không có JD phù hợp sẽ không xuất hiện. */
    @Query("select j.companyId, count(j) from Job j where j.companyId in :ids and j.status <> :excluded group by j.companyId")
    List<Object[]> countByCompanyIdsExcludingStatus(@Param("ids") Collection<UUID> ids,
                                                    @Param("excluded") JobStatus excluded);

    /** Mỗi dòng là [status, số JD]; trạng thái không có JD sẽ không xuất hiện. */
    @Query("select j.status, count(j) from Job j where j.companyId = :companyId group by j.status")
    List<Object[]> countByStatus(@Param("companyId") UUID companyId);

    /**
     * Đóng hàng loạt JD APPROVED có hạn nộp trước {@code today}. Truy vấn hàng loạt bỏ qua @Version/@PreUpdate nên
     * tự tăng version và đặt updatedAt; không đụng reviewed_*. Trả về số dòng đã đóng.
     */
    @Modifying
    @Query("update Job j set j.status = :closed, j.version = j.version + 1, j.updatedAt = :now "
            + "where j.status = :approved and j.deadline < :today")
    int closeExpiredApproved(@Param("today") LocalDate today, @Param("now") Instant now,
                             @Param("approved") JobStatus approved, @Param("closed") JobStatus closed);

    long countByCompanyIdAndStatusAndDeadlineBetween(UUID companyId, JobStatus status, LocalDate from, LocalDate to);

    /**
     * Mỗi dòng là [companyId, số tin đang hiển thị với ứng viên] (APPROVED, chưa quá hạn); không lọc trạng thái công ty,
     * nơi gọi tự bỏ công ty không ACTIVE.
     */
    @Query("select j.companyId, count(j) from Job j where j.status = :status "
            + "and (j.deadline is null or j.deadline >= :today) group by j.companyId")
    List<Object[]> countOpenByCompany(@Param("status") JobStatus status, @Param("today") LocalDate today);

    /** DN-07: mỗi dòng là [status, số JD] trên mọi công ty, bỏ trạng thái {@code excluded} (DRAFT). */
    @Query("select j.status, count(j) from Job j where j.status <> :excluded group by j.status")
    List<Object[]> countAllByStatusExcluding(@Param("excluded") JobStatus excluded);
}
