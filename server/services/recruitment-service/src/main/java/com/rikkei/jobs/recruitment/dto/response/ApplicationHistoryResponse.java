package com.rikkei.jobs.recruitment.dto.response;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;
public record ApplicationHistoryResponse(UUID id, ApplicationStatus fromStatus, ApplicationStatus toStatus,
    UUID actorId, String note, Instant createdAt) {}
