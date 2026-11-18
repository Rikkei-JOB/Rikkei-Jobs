package com.rikkei.jobs.recruitment.dto.response;

import com.rikkei.jobs.recruitment.client.CandidateIdentity;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record HrApplicationDetailResponse(UUID id, UUID jobId, String jobTitle, UUID companyId,
    String companyName, ApplicationStatus status, Instant createdAt, Instant updatedAt,
    CandidateIdentity candidate, UUID cvId, String cvSnapshotUrl, String coverLetter,
    MatchResponse match, List<InterviewRoundResponse> rounds,
    List<InterviewRoundRevisionResponse> roundRevisions,
    List<ApplicationHistoryResponse> history, Set<ApplicationStatus> allowedTransitions,
    OfferResponse offer, Instant onboardConfirmedAt) {}
