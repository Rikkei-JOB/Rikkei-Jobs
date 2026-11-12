package com.rikkei.jobs.recruitment.service.impl;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.recruitment.dto.integration.JobApplicationContext;
import com.rikkei.jobs.recruitment.entity.*;
import com.rikkei.jobs.recruitment.repository.*;
import com.rikkei.jobs.recruitment.service.ApplicationWriteService;
import java.time.Clock;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Slf4j
public class ApplicationWriteServiceImpl implements ApplicationWriteService {
    private final ApplicationRepository applications;
    private final ApplicationStatusHistoryRepository histories;
    private final ApplicationNotificationOutboxRepository outbox;
    private final Clock clock;

    @Override @Transactional
    public Application createSubmitted(UUID applicationId, UUID userId, UUID cvId, JobApplicationContext context, String coverLetter) {
        var now = clock.instant();
        if (!context.deadline().isAfter(now)) throw new ConflictException("Vị trí đã hết hạn ứng tuyển.");
        var a = new Application();
        a.setId(applicationId); a.setJobId(context.jobId()); a.setUserId(userId); a.setCompanyId(context.companyId());
        a.setJobTitle(context.jobTitle()); a.setCompanyName(context.companyName()); a.setCvId(cvId);
        a.setCvSnapshotUrl("/api/v1/recruitment/applications/" + applicationId + "/cv");
        a.setCoverLetter(coverLetter); a.setStatus(ApplicationStatus.SUBMITTED); a.setCreatedAt(now); a.setUpdatedAt(now);
        applications.saveAndFlush(a);
        var history = new ApplicationStatusHistory();
        history.setId(UUID.randomUUID()); history.setApplicationId(applicationId); history.setToStatus(ApplicationStatus.SUBMITTED);
        history.setActorId(userId); history.setCreatedAt(now); histories.save(history);
        var recipients = context.hrEmails().stream().map(String::strip).map(s -> s.toLowerCase(Locale.ROOT)).filter(s -> !s.isEmpty()).distinct().toList();
        if (recipients.isEmpty()) log.warn("No active HR recipient for application {}", applicationId);
        for (String recipient : recipients) {
            var notification = new ApplicationNotificationOutbox();
            notification.setId(UUID.randomUUID()); notification.setApplicationId(applicationId); notification.setRecipient(recipient);
            notification.setStatus("PENDING"); notification.setCreatedAt(now); notification.setNextAttemptAt(now);
            outbox.save(notification);
        }
        return a;
    }
}
