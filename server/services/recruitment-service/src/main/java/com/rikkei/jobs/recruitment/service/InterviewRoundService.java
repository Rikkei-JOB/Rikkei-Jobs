package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.common.config.exception.*;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.recruitment.dto.request.*;
import com.rikkei.jobs.recruitment.dto.response.InterviewRoundResponse;
import com.rikkei.jobs.recruitment.dto.response.MentorRoundResponse;
import com.rikkei.jobs.recruitment.client.MentorIdentityClient;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.recruitment.entity.*;
import com.rikkei.jobs.recruitment.repository.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class InterviewRoundService {
    private final ApplicationRepository applications;
    private final InterviewRoundRepository rounds;
    private final InterviewRoundRevisionRepository revisions;
    private final Clock clock;
    private final MentorIdentityClient mentors;
    private final InterviewLifecycleService interviewLifecycle;

    @Transactional
    public InterviewRoundResponse create(AuthPrincipal actor, UUID applicationId, CreateRoundRequest request) {
        requireEditable(actor, applicationId);
        if (request == null || request.name() == null || request.name().isBlank()
            || request.interviewerEmail() == null || request.interviewerEmail().isBlank()) {
            throw new BadRequestException("Thông tin vòng phỏng vấn không hợp lệ.");
        }
        var round = new InterviewRound();
        round.setId(UUID.randomUUID()); round.setApplicationId(applicationId);
        round.setRoundNo(rounds.maxRoundNo(applicationId) + 1);
        round.setName(request.name().strip());
        round.setInterviewerEmail(request.interviewerEmail().strip());
        round.setScheduledAt(request.scheduledAt());
        round.setResult(InterviewRoundResult.PENDING);
        round.setCreatedAt(clock.instant());
        return toResponse(rounds.saveAndFlush(round));
    }

    @Transactional
    public InterviewRoundResponse update(AuthPrincipal actor, UUID applicationId, UUID roundId, UpdateRoundRequest request) {
        requireEditable(actor, applicationId);
        var round = rounds.findByIdAndApplicationId(roundId, applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vòng phỏng vấn."));
        return saveResult(actor, round, request);
    }

    @Transactional(readOnly = true)
    public PageResponse<MentorRoundResponse> listInvited(AuthPrincipal actor, int page, int size) {
        requireMentor(actor);
        if (page < 1 || size < 1 || size > 100) throw new BadRequestException("page/size không hợp lệ.");
        String email = mentors.email(actor.getUserId());
        var result = rounds.findByInterviewerEmailIgnoreCase(email,
            PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        var items = result.getContent().stream().map(round -> {
            var application = applications.findById(round.getApplicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển."));
            return new MentorRoundResponse(application.getJobTitle(), application.getCompanyName(),
                application.getStatus(), toResponse(round));
        }).toList();
        return PageResponse.of(result, items);
    }

    @Transactional
    public InterviewRoundResponse updateInvited(AuthPrincipal actor, UUID roundId, UpdateRoundRequest request) {
        requireMentor(actor);
        String email = mentors.email(actor.getUserId());
        UUID applicationId = rounds.findApplicationIdByRoundId(roundId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vòng phỏng vấn."));
        var application = applications.findForUpdate(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vòng phỏng vấn."));
        // Load the round after the application lock so revisions see the last committed result.
        var round = rounds.findById(roundId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vòng phỏng vấn."));
        if (!round.getInterviewerEmail().equalsIgnoreCase(email))
            throw new ResourceNotFoundException("Không tìm thấy vòng phỏng vấn.");
        if (application.getStatus() != ApplicationStatus.INTERVIEW)
            throw new ConflictException("Chỉ có thể sửa vòng khi đơn đang phỏng vấn.");
        return saveResult(actor, round, request);
    }

    private void requireMentor(AuthPrincipal actor) {
        if (actor == null || actor.getRole() != Role.MENTOR)
            throw new ForbiddenException("Chỉ mentor được ghi vòng được mời.");
    }

    private InterviewRoundResponse saveResult(AuthPrincipal actor, InterviewRound round, UpdateRoundRequest request) {
        UUID applicationId = round.getApplicationId();
        UUID roundId = round.getId();
        if (request == null || request.result() == null) throw new BadRequestException("Kết quả vòng phỏng vấn không hợp lệ.");
        BigDecimal score = request.result() == InterviewRoundResult.PENDING ? null : request.score();
        if (score != null && (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new BadRequestException("Điểm vòng phỏng vấn phải từ 0 đến 100.");
        }
        String note = request.note() == null ? null : request.note().strip();
        if (note != null && note.length() > 2000) throw new BadRequestException("Ghi chú vòng phỏng vấn quá dài.");
        if (round.getResult() != request.result() || !Objects.equals(round.getScore(), score)
            || !Objects.equals(round.getNote(), note)) {
            var revision = new InterviewRoundRevision();
            revision.setId(UUID.randomUUID()); revision.setRoundId(roundId); revision.setApplicationId(applicationId);
            revision.setActorId(actor.getUserId()); revision.setCreatedAt(clock.instant());
            revision.setOldResult(round.getResult()); revision.setNewResult(request.result());
            revision.setOldScore(round.getScore()); revision.setNewScore(score);
            revision.setOldNote(round.getNote()); revision.setNewNote(note);
            revisions.save(revision);
            round.setResult(request.result()); round.setScore(score); round.setNote(note);
        }
        if (request.result() != InterviewRoundResult.PENDING) interviewLifecycle.completeForRound(roundId, clock.instant());
        return toResponse(rounds.saveAndFlush(round));
    }

    @Transactional(readOnly = true)
    public List<InterviewRoundResponse> list(UUID applicationId) {
        return rounds.findByApplicationIdOrderByRoundNoAsc(applicationId).stream().map(this::toResponse).toList();
    }

    private void requireEditable(AuthPrincipal actor, UUID applicationId) {
        if (actor == null || actor.getRole() != Role.HR) throw new ForbiddenException("Chỉ HR được sửa vòng phỏng vấn.");
        var application = applications.findForUpdate(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển."));
        if (!application.getCompanyId().equals(actor.getCompanyId()))
            throw new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển.");
        if (application.getStatus() != ApplicationStatus.INTERVIEW)
            throw new ConflictException("Chỉ có thể sửa vòng khi đơn đang phỏng vấn.");
    }

    private InterviewRoundResponse toResponse(InterviewRound round) {
        return new InterviewRoundResponse(round.getId(), round.getApplicationId(), round.getRoundNo(),
            round.getName(), round.getInterviewerEmail(), round.getScheduledAt(), round.getResult(),
            round.getScore(), round.getNote(), round.getCreatedAt());
    }
}
