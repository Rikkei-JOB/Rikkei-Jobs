package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobStatus;
import java.time.LocalDate;
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

    /*
     * Quy tắc "ứng viên thấy JD nào" (DN-07 §4.2) chỉ viết ở hai truy vấn dưới: JD APPROVED, hạn nộp từ hôm nay
     * (giờ Việt Nam, nơi gọi truyền JobRules.today) và công ty ACTIVE. GET /jobs, chi tiết công khai và DN-04 dùng chung.
     */
    String VISIBLE_TO_CANDIDATES = "from Job j join Company c on c.id = j.companyId "
            + "where j.status = com.rikkei.jobs.company.entity.JobStatus.APPROVED and j.deadline >= :today "
            + "and c.status = com.rikkei.jobs.company.entity.CompanyStatus.ACTIVE";

    /** Mới duyệt trước ({@code reviewedAt}, dữ liệu cũ thiếu thì {@code updatedAt}), rồi id; {@code p} không mang sort. */
    @Query(value = "select j " + VISIBLE_TO_CANDIDATES + " order by coalesce(j.reviewedAt, j.updatedAt) desc, j.id desc",
            countQuery = "select count(j) " + VISIBLE_TO_CANDIDATES)
    Page<Job> findVisibleToCandidates(@Param("today") LocalDate today, Pageable p);

    @Query("select j " + VISIBLE_TO_CANDIDATES + " and j.id = :id")
    Optional<Job> findVisibleToCandidatesById(@Param("id") UUID id, @Param("today") LocalDate today);

    /** Mỗi dòng là [status, số JD]; trạng thái không có JD sẽ không xuất hiện. */
    @Query("select j.status, count(j) from Job j where j.companyId = :companyId group by j.status")
    List<Object[]> countByStatus(@Param("companyId") UUID companyId);

    long countByCompanyIdAndStatusAndDeadlineBetween(UUID companyId, JobStatus status, LocalDate from, LocalDate to);
}
