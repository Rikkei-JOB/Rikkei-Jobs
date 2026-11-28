package com.rikkei.jobs.company.service;

import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mỗi ngày 00:05 (giờ Việt Nam) đóng các JD APPROVED đã quá hạn nộp. Không ghi lịch sử công ty
 * (đây là thao tác hệ thống) và không đổi reviewed_*.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.jobs.auto-close.enabled", havingValue = "true", matchIfMissing = true)
public class JobAutoCloseScheduler {

    private final JobRepository jobRepository;
    private final Clock clock;

    /** Trả về số JD đã đóng. */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public int closeExpired() {
        LocalDate today = JobRules.today(clock);
        int closed = jobRepository.closeExpiredApproved(today, Instant.now(clock), JobStatus.APPROVED, JobStatus.CLOSED);
        log.info("Tự đóng {} JD quá hạn nộp (hạn trước {})", closed, today);
        return closed;
    }
}
