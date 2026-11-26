package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.Language;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<Language, UUID> {
    /** Trình độ lưu dạng chuỗi A1..C2 nên sắp chuỗi giảm dần là trình độ cao trước. */
    List<Language> findAllByUserIdOrderByLevelDescNameAsc(UUID userId);
    Optional<Language> findByIdAndUserId(UUID id, UUID userId);
    List<Language> findAllByUserIdAndNameIgnoreCase(UUID userId, String name);
}
