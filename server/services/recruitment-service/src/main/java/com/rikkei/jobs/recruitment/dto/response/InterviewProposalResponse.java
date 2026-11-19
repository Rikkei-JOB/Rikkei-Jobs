package com.rikkei.jobs.recruitment.dto.response;
import java.util.*;
import java.time.*;
public record InterviewProposalResponse(UUID proposalId, UUID roundId, List<InterviewSlotResponse> slots) {}
