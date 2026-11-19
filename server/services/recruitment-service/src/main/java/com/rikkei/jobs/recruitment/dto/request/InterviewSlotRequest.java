package com.rikkei.jobs.recruitment.dto.request;
import java.util.*;
import java.time.*;
public record InterviewSlotRequest(OffsetDateTime startAt, OffsetDateTime endAt) {}
