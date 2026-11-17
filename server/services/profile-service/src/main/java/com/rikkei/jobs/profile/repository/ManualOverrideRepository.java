package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.ManualOverride;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ManualOverrideRepository extends JpaRepository<ManualOverride, UUID> {
    List<ManualOverride> findAllByRecordIdInOrderByCreatedAtDesc(Collection<UUID> recordIds);
}
