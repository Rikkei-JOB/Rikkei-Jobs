package com.rikkei.jobs.recruitment.dto.response;
import java.util.*;
import java.time.*;
public record InterviewScheduleResponse(UUID id, UUID roundId, UUID slotId, Instant startAt, Instant endAt, String meetUrl, String status) {}
