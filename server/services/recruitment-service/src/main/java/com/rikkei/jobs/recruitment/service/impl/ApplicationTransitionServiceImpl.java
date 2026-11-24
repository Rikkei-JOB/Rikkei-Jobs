package com.rikkei.jobs.recruitment.service.impl;

import com.rikkei.jobs.common.config.exception.*;
import com.rikkei.jobs.recruitment.entity.*;
import com.rikkei.jobs.recruitment.repository.*;
import com.rikkei.jobs.recruitment.service.*;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ApplicationTransitionServiceImpl implements ApplicationTransitionService {
    private final ApplicationRepository applications;
    private final com.rikkei.jobs.recruitment.service.InterviewLifecycleService interviewLifecycle;
    private final ApplicationStatusHistoryRepository histories;
    private final ApplicationStatusNotificationRepository statusNotifications;
    private final CandidateNotificationService candidateNotifications;
    private final InterviewRoundRepository rounds;
    private final ApplicationStatusPolicy policy;
    private final OfferEligibilityPolicy offerEligibility;
    private final Clock clock;

    @Override @Transactional
    public Application transition(UUID id, ApplicationStatus target, UUID actorId, String note) {
        var a = locked(id);
        if (target == ApplicationStatus.WITHDRAWN && !a.getUserId().equals(actorId)) throw new ForbiddenException("Chỉ chủ đơn được rút đơn.");
        return change(a, target, actorId, note);
    }

    @Override @Transactional
    public Application withdraw(UUID id, UUID userId, String reason) {
        var a = locked(id);
        if (!a.getUserId().equals(userId)) throw new ForbiddenException("Bạn không có quyền rút đơn này.");
        if (reason == null || reason.isBlank() || reason.strip().length() > 1000) throw new BadRequestException("Lý do rút đơn phải từ 1 đến 1.000 ký tự.");
        return change(a, ApplicationStatus.WITHDRAWN, userId, reason.strip());
    }

    private Application locked(UUID id) {
        return applications.findForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển."));
    }
    private Application change(Application a, ApplicationStatus target, UUID actorId, String note) {
        policy.requireTransition(a.getStatus(), target);
        if (target == ApplicationStatus.OFFER) {
            offerEligibility.requireEligible(rounds.findByApplicationIdOrderByRoundNoAsc(a.getId()));
        }
        if (actorId == null || (note != null && note.length() > 1000)) throw new BadRequestException("Dữ liệu lịch sử không hợp lệ.");
        var now = clock.instant();
        // PostgreSQL stores microseconds. Preserve chronology even with a fixed or adjusted clock.
        if (!now.isAfter(a.getUpdatedAt())) now = a.getUpdatedAt().plusNanos(1000);
        var history = new ApplicationStatusHistory();
        history.setId(UUID.randomUUID()); history.setApplicationId(a.getId()); history.setFromStatus(a.getStatus());
        history.setToStatus(target); history.setActorId(actorId); history.setNote(note); history.setCreatedAt(now);
        if (a.getStatus() == ApplicationStatus.INTERVIEW && target != ApplicationStatus.INTERVIEW) interviewLifecycle.cancelForApplication(a.getId(), now);
        a.setStatus(target); a.setUpdatedAt(now); histories.saveAndFlush(history);
        var notification = new ApplicationStatusNotification();
        notification.setId(UUID.randomUUID()); notification.setStatusHistoryId(history.getId());
        notification.setApplicationId(a.getId()); notification.setUserId(a.getUserId());
        notification.setStatus("PENDING"); notification.setCreatedAt(now); notification.setNextAttemptAt(now);
        statusNotifications.save(notification);
        candidateNotifications.statusChanged(a, history.getId(), target, now);
        return applications.saveAndFlush(a);
    }
}
