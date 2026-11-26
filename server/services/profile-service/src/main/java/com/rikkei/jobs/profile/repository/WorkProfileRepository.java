package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.WorkProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkProfileRepository extends JpaRepository<WorkProfile, UUID> {
}
