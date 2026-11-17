package com.rikkei.jobs.recruitment.dto.response;

import com.rikkei.jobs.recruitment.entity.InterviewRoundResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InterviewRoundResponse(UUID id, UUID applicationId, int roundNo, String name,
    String interviewerEmail, Instant scheduledAt, InterviewRoundResult result,
    BigDecimal score, String note, Instant createdAt) {}
