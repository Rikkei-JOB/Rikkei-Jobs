package com.rikkei.jobs.recruitment.dto.response;

import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import java.time.Instant;
import java.util.UUID;

public record ApplicationWorkflowResponse(UUID id, ApplicationStatus status, Instant updatedAt) {}
