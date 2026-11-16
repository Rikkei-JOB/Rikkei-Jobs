package com.rikkei.jobs.profile.repository;

import com.rikkei.jobs.profile.entity.CandidateProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> { }
