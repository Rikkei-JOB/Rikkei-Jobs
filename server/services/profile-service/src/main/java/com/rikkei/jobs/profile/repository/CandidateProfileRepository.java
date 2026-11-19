package com.rikkei.jobs.profile.repository;

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

    Page<CandidateProfile> findByVisibility(String visibility, Pageable pageable);
}
