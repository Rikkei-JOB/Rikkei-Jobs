package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.recruitment.dto.integration.JobApplicationContext;
import com.rikkei.jobs.recruitment.entity.Application;
import java.util.UUID;
public interface ApplicationWriteService {
    Application createSubmitted(UUID applicationId, UUID userId, UUID cvId, JobApplicationContext context, String coverLetter);
}
