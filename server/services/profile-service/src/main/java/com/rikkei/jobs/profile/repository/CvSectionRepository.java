package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.CvSection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CvSectionRepository extends JpaRepository<CvSection, CvSection.Key> {
    List<CvSection> findAllByCvIdOrderBySortOrderAsc(UUID cvId);
    void deleteAllByCvId(UUID cvId);
}
