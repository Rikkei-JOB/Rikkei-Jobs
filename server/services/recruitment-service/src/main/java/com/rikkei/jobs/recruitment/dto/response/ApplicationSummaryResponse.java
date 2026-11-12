package com.rikkei.jobs.recruitment.dto.response;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;
public record ApplicationSummaryResponse(UUID id, UUID jobId, String jobTitle, UUID companyId,
    String companyName, ApplicationStatus status, Instant createdAt, Instant updatedAt, boolean canWithdraw) {}
