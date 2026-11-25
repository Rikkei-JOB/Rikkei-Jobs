package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.Cv;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CvRepository extends JpaRepository<Cv, UUID> {
    List<Cv> findAllByUserIdOrderByCreatedAtAscIdAsc(UUID userId);
    Optional<Cv> findByIdAndUserId(UUID id, UUID userId);
    Optional<Cv> findByPublicToken(String publicToken);
    long countByUserId(UUID userId);
}
