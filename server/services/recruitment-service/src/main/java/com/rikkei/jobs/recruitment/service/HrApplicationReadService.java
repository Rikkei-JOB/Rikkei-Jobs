package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ForbiddenException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.recruitment.client.IdentityCandidateClient;
import com.rikkei.jobs.recruitment.client.CandidateContactClient;
import com.rikkei.jobs.recruitment.client.CandidateIdentity;
import com.rikkei.jobs.recruitment.client.JobMatchClient;
import com.rikkei.jobs.recruitment.dto.response.*;
import com.rikkei.jobs.recruitment.entity.*;
import com.rikkei.jobs.recruitment.repository.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class HrApplicationReadService {
    private final ApplicationRepository applications;
    private final ApplicationStatusHistoryRepository histories;
    private final InterviewRoundRepository rounds;
    private final InterviewRoundRevisionRepository revisions;
    private final IdentityCandidateClient identities;
    private final CandidateContactClient contacts;
    private final MatchLookupService matches;
    private final ApplicationReadAccessService access;
    private final HrTransitionPolicy policy;
    private final OfferRepository offers;
    private final EmploymentRecordRepository employmentRecords;
    private final InterviewQueryService interviewQueries;

    @Transactional(readOnly = true)
    public HrApplicationDetailResponse detail(AuthPrincipal actor, UUID id, String bearer) {
        var application = access.findReadable(actor,id);
        var identity = identities.get(application.getUserId());
        var candidate = actor.getRole() == Role.HR
            ? new CandidateIdentity(identity.userId(), identity.fullName(), null, null)
            : new CandidateIdentity(identity.userId(), identity.fullName(), identity.email(),
                contacts.phone(application.getUserId(), bearer));
        // DN-06 authorizes HR and STAFF, but not ADMIN. ADMIN can inspect the workflow without a match lookup.
        var match = actor.getRole() == Role.ADMIN ? new MatchLookupResponse(MatchStatus.NOT_REQUESTED,null)
            : matches.lookup(application.getJobId(), application.getUserId(), bearer);
        var roundEntities = rounds.findByApplicationIdOrderByRoundNoAsc(id);
        var roundIds = roundEntities.stream().map(InterviewRound::getId).toList();
        var slotData = actor.getRole() == Role.HR ? interviewQueries.slotMap(roundIds) : java.util.Map.<UUID,java.util.List<InterviewSlotResponse>>of();
        var scheduleData = actor.getRole() == Role.HR ? interviewQueries.scheduleMap(roundIds) : java.util.Map.<UUID,InterviewScheduleResponse>of();
        var roundData = roundEntities.stream()
            .map(r -> new InterviewRoundResponse(r.getId(), r.getApplicationId(), r.getRoundNo(), r.getName(),
                r.getInterviewerEmail(), r.getScheduledAt(), r.getResult(), r.getScore(), r.getNote(), r.getCreatedAt(), slotData.getOrDefault(r.getId(), java.util.List.of()), scheduleData.get(r.getId()))).toList();
        var revisionData = revisions.findByApplicationIdOrderByCreatedAtAscIdAsc(id).stream()
            .map(r -> new InterviewRoundRevisionResponse(r.getId(), r.getRoundId(), r.getActorId(), r.getOldResult(),
                r.getNewResult(), r.getOldScore(), r.getNewScore(), r.getOldNote(), r.getNewNote(), r.getCreatedAt())).toList();
        var historyData = histories.findByApplicationIdOrderByCreatedAtAscIdAsc(id).stream()
            .map(h -> new ApplicationHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(),
                h.getActorId(), h.getNote(), h.getCreatedAt())).toList();
        return new HrApplicationDetailResponse(id, application.getJobId(), application.getJobTitle(),
            application.getCompanyId(), application.getCompanyName(), application.getStatus(),
            application.getCreatedAt(), application.getUpdatedAt(), candidate, application.getCvId(),
            application.getCvSnapshotUrl(), application.getCoverLetter(), match.match(), roundData, revisionData,
            historyData, actor.getRole() == Role.HR ? policy.allowedFrom(application.getStatus()) : java.util.Set.of(),
            actor.getRole() == Role.HR ? offers.findByApplicationId(id).map(OfferResponse::from).orElse(null) : null,
            actor.getRole() == Role.HR ? employmentRecords.findByApplicationId(id)
                .map(EmploymentRecord::getConfirmedAt).orElse(null) : null, match.matchStatus());
    }

    @Transactional(readOnly = true)
    public PageResponse<HrApplicationSummaryResponse> listHr(AuthPrincipal actor, UUID jobId,
        ApplicationStatus status, int page, int size) {
        if (actor == null || actor.getRole() != Role.HR || actor.getCompanyId() == null)
            throw new ForbiddenException("Chỉ HR được xem đơn của công ty.");
        return list(actor.getCompanyId(), jobId, status, page, size);
    }

    @Transactional(readOnly = true)
    public PageResponse<HrApplicationSummaryResponse> listStaff(AuthPrincipal actor, UUID companyId,
        UUID jobId, ApplicationStatus status, int page, int size) {
        if (actor == null || (actor.getRole() != Role.STAFF && actor.getRole() != Role.ADMIN))
            throw new ForbiddenException("Chỉ STAFF hoặc ADMIN được xem toàn bộ đơn.");
        return list(companyId, jobId, status, page, size);
    }

    private PageResponse<HrApplicationSummaryResponse> list(UUID companyId, UUID jobId,
        ApplicationStatus status, int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new BadRequestException("page/size không hợp lệ.");
        var result = applications.findVisible(companyId, jobId, status,
            PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        var ids = new ArrayList<>(new LinkedHashSet<>(result.getContent().stream().map(Application::getUserId).toList()));
        var candidates = identities.getBatch(ids);
        var summaries = result.getContent().stream().map(a -> {
            var candidate = candidates.get(a.getUserId());
            if (candidate == null) throw new com.rikkei.jobs.recruitment.config.RecruitmentDependencyException(
                502, "INVALID_CANDIDATE_IDENTITY", "Thông tin ứng viên không hợp lệ.");
            return new HrApplicationSummaryResponse(a.getId(), a.getJobId(), a.getJobTitle(), a.getCompanyId(),
                a.getCompanyName(), a.getUserId(), candidate.fullName(), a.getStatus(), a.getCreatedAt(), a.getUpdatedAt());
        }).toList();
        return PageResponse.of(result, summaries);
    }

}
