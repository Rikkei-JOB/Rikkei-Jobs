package com.rikkei.jobs.recruitment.dto.response;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import java.time.Instant;
import java.util.*;
public record ApplicationDetailResponse(UUID id, UUID jobId, String jobTitle, UUID companyId,
    String companyName, ApplicationStatus status, Instant createdAt, Instant updatedAt, boolean canWithdraw,
    UUID cvId, String cvSnapshotUrl, String coverLetter, List<ApplicationHistoryResponse> history) {}
