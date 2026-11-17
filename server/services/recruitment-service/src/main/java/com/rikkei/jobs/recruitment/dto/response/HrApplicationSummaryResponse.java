package com.rikkei.jobs.recruitment.dto.response;

import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record HrApplicationSummaryResponse(UUID id, UUID jobId, String jobTitle, UUID companyId,
    String companyName, UUID userId, String candidateName, ApplicationStatus status,
    Instant createdAt, Instant updatedAt) {}
