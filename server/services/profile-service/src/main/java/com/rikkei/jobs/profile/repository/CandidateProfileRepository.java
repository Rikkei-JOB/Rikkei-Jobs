package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.dto.StaffReviewRow;
import com.rikkei.jobs.profile.entity.CandidateProfile;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {
    @Query(value = "select exists (select 1 from profile.public_candidate_ids where user_id = :userId)", nativeQuery = true)
    boolean isPublic(@Param("userId") UUID userId);

    String QUEUE_FILTER = """
        where p.visibility = :status
          and (:q = '' or locate(lower(:q), lower(p.fullName)) > 0)
          and (:classCode = '' or lower(p.classCode) = lower(:classCode))
          and (:courseName = '' or lower(p.courseName) = lower(:courseName))
        """;

    @Query(value = """
        select new com.rikkei.jobs.profile.dto.StaffReviewRow(
            p.userId, p.fullName, p.classCode, p.courseName, p.consentAt, p.visibility, count(r.id))
        from CandidateProfile p left join ProfileReview r on r.userId = p.userId and r.action = REJECT
        """ + QUEUE_FILTER + """
        group by p.userId, p.fullName, p.classCode, p.courseName, p.consentAt, p.visibility
        order by p.consentAt, p.userId
        """, countQuery = "select count(p) from CandidateProfile p " + QUEUE_FILTER)
    Page<StaffReviewRow> reviewQueue(@Param("status") String status, @Param("q") String q,
        @Param("classCode") String classCode, @Param("courseName") String courseName, Pageable pageable);
}
