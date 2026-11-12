package com.rikkei.jobs.recruitment.service.impl;

import com.rikkei.jobs.common.config.exception.*;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.recruitment.client.*;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.rikkei.jobs.recruitment.dto.integration.JobApplicationContext;
import com.rikkei.jobs.recruitment.dto.request.*;
import com.rikkei.jobs.recruitment.dto.response.*;
import com.rikkei.jobs.recruitment.entity.*;
import com.rikkei.jobs.recruitment.mapper.ApplicationMapper;
import com.rikkei.jobs.recruitment.repository.*;
import com.rikkei.jobs.recruitment.service.*;
import java.time.Clock;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Slf4j
public class ApplicationServiceImpl implements ApplicationService {
    private final ApplicationRepository applications;
    private final ApplicationStatusHistoryRepository histories;
    private final JobApplicationClient jobs;
    private final ProfileCvClient cvs;
    private final CvSnapshotStorage snapshots;
    private final ApplicationWriteService writer;
    private final ApplicationTransitionService transitions;
    private final ApplicationMapper mapper;
    private final Clock clock;

    @Override
    public ApplicationDetailResponse apply(AuthPrincipal actor, String bearerToken, UUID jobId, ApplyRequest request) {
        requireCandidate(actor);
        if (request.cvId() == null || (request.coverLetter() != null && request.coverLetter().length() > 5000)) throw new BadRequestException("CV hoặc thư giới thiệu không hợp lệ.");
        if (applications.existsByUserIdAndJobIdAndStatusNotIn(actor.getUserId(), jobId, List.of(ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN))) {
            throw new ConflictException("Bạn đã có đơn ứng tuyển cho vị trí này.");
        }
        validateJob(jobId, jobs.getContext(jobId));
        var pdf = cvs.exportOwnedPdf(request.cvId(), bearerToken);
        UUID id = UUID.randomUUID();
        snapshots.store(id, pdf.bytes());
        try {
            var current = jobs.getContext(jobId);
            validateJob(jobId, current);
            writer.createSubmitted(id, actor.getUserId(), request.cvId(), current, request.coverLetter());
        } catch (RuntimeException ex) {
            try { snapshots.delete(id); }
            catch (RuntimeException cleanup) { log.error("Orphan CV snapshot requires cleanup for application {}", id); }
            if (ex instanceof DataIntegrityViolationException integrity && isDuplicateApplication(integrity)) {
                throw new ConflictException("Bạn đã có đơn ứng tuyển cho vị trí này.");
            }
            throw ex;
        }
        // A read failure after the write committed must never remove the committed snapshot.
        return getOwn(actor.getUserId(), id);
    }

    private boolean isDuplicateApplication(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                && "uq_applications_user_job_active".equals(violation.getConstraintName())) return true;
        }
        return false;
    }
    private void validateJob(UUID jobId, JobApplicationContext context) {
        if (context == null || !jobId.equals(context.jobId()) || context.companyId() == null || context.deadline() == null ||
            context.companyLocked() == null || context.jobTitle() == null || context.companyName() == null || context.status() == null || context.hrEmails() == null) {
            throw new RecruitmentDependencyException(502, "INVALID_JOB_CONTEXT", "Không xác minh được thông tin vị trí tuyển dụng.");
        }
        if (!"APPROVED".equals(context.status()) || context.companyLocked() || !context.deadline().isAfter(clock.instant())) {
            throw new ConflictException("Vị trí hiện không nhận hồ sơ ứng tuyển.");
        }
    }

    @Override @Transactional(readOnly=true)
    public PageResponse<ApplicationSummaryResponse> listOwn(UUID userId, ApplicationStatus status, int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new BadRequestException("Phân trang không hợp lệ.");
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        var result = status == null ? applications.findByUserId(userId, pageable) : applications.findByUserIdAndStatus(userId, status, pageable);
        return PageResponse.of(result, result.stream().map(mapper::summary).toList());
    }

    @Override @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public ApplicationDetailResponse getOwn(UUID userId, UUID id) {
        var a = get(id);
        if (!a.getUserId().equals(userId)) throw new ForbiddenException("Bạn không có quyền xem đơn này.");
        return mapper.detail(a, histories.findByApplicationIdOrderByCreatedAtAscIdAsc(id));
    }

    @Override
    public ApplicationDetailResponse withdraw(AuthPrincipal actor, UUID id, WithdrawRequest request) {
        requireCandidate(actor);
        transitions.withdraw(id, actor.getUserId(), request.reason());
        return getOwn(actor.getUserId(), id);
    }

    @Override @Transactional(readOnly=true)
    public byte[] readSnapshot(AuthPrincipal actor, UUID id) {
        var a = get(id);
        boolean owner = actor.getRole() == Role.CANDIDATE && a.getUserId().equals(actor.getUserId());
        boolean hr = actor.getRole() == Role.HR && a.getCompanyId().equals(actor.getCompanyId());
        if (!owner && !hr) throw new ForbiddenException("Bạn không có quyền xem CV của đơn này.");
        return snapshots.read(id);
    }
    private Application get(UUID id) {
        return applications.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển."));
    }
    private void requireCandidate(AuthPrincipal actor) {
        if (actor.getRole() != Role.CANDIDATE) throw new ForbiddenException("Chỉ ứng viên được thực hiện thao tác này.");
    }
}
