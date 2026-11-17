package com.rikkei.jobs.company.repository;

import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobSkillId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JobSkillRepository extends JpaRepository<JobSkill, JobSkillId> {

    List<JobSkill> findByJobId(UUID jobId);

    @Modifying
    @Query("delete from JobSkill s where s.jobId = :jobId")
    void deleteByJobId(@Param("jobId") UUID jobId);
}
