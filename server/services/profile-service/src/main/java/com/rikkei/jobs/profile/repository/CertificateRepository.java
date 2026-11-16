package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.Certificate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, UUID> {
    List<Certificate> findAllByUserIdOrderByIssuedAtDescIdAsc(UUID userId);
    Optional<Certificate> findByIdAndUserId(UUID id, UUID userId);
}
