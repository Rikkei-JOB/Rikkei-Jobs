package com.rikkei.jobs.recruitment.service;
import java.util.*;
import java.time.*;
public record InterviewNotificationPayload(UUID scheduleId, UUID roundId, String title, String companyName, String candidateName, String interviewerEmail, Instant startAt, Instant endAt, String meetUrl) {}
