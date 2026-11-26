package com.rikkei.jobs.profile.dto;

import java.time.Instant;
import java.util.UUID;

public record StaffReviewRow(UUID userId, String fullName, String classCode, String courseName,
                            Instant consentAt, String visibility, long rejectCount) {}
