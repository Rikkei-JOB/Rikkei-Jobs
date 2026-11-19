package com.rikkei.jobs.recruitment.dto.request;
import java.util.*;
import java.time.*;
public record InterviewSlotRequest(
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=InterviewTimeDeserializer.class) OffsetDateTime startAt,
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=InterviewTimeDeserializer.class) OffsetDateTime endAt) {}
