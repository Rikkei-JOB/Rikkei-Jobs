package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.ProfileReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileReviewRepository extends JpaRepository<ProfileReview, Long> {
    List<ProfileReview> findByUserIdOrderByIdDesc(UUID userId);
}
