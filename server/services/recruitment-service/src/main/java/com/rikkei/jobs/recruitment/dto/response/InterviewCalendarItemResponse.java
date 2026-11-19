package com.rikkei.jobs.recruitment.dto.response;
import java.util.*;
import java.time.*;
public record InterviewCalendarItemResponse(InterviewScheduleResponse schedule, UUID candidateId, String candidateName, String jobTitle, String roundName, String interviewerEmail) {}
