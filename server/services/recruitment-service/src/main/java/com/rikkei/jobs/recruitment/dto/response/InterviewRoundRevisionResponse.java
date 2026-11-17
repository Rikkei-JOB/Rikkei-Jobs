package com.rikkei.jobs.recruitment.dto.response;

import com.rikkei.jobs.recruitment.entity.InterviewRoundResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InterviewRoundRevisionResponse(UUID id, UUID roundId, UUID actorId,
    InterviewRoundResult oldResult, InterviewRoundResult newResult, BigDecimal oldScore,
    BigDecimal newScore, String oldNote, String newNote, Instant createdAt) {}
