package com.rikkei.jobs.recruitment.dto.response;
import java.util.*;
import java.time.*;
public record InterviewInvitationResponse(UUID roundId, String roundName, String jobTitle, String companyName, String interviewerEmail, List<InterviewSlotResponse> slots, InterviewScheduleResponse schedule) {}
