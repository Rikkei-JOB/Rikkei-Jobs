package com.rikkei.jobs.recruitment.dto.response;
import java.util.*;
import java.time.*;
public record InterviewSlotResponse(UUID id, UUID roundId, UUID proposalId, Instant startAt, Instant endAt, String status) {}
