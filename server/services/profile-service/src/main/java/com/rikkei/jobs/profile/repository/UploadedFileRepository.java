package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.UploadedFile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {
    Optional<UploadedFile> findByIdAndUserId(UUID id, UUID userId);
}
