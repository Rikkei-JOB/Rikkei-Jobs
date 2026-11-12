package com.rikkei.jobs.profile.service.sync;

import com.rikkei.jobs.common.config.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Job đêm NT-05. Giờ chạy lấy từ {@code app.sync.nightly-cron} (biến môi trường SYNC_NIGHTLY_CRON, mặc định 01:00
 * giờ Việt Nam); đặt "-" để tắt. Chạy tay: {@code POST /staff/sync/nightly}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NightlySyncJob {
    private final SyncService sync;

    @Scheduled(cron = "${app.sync.nightly-cron:0 0 1 * * *}", zone = "${app.sync.zone:Asia/Ho_Chi_Minh}")
    public void run() {
        try {
            sync.runNightly(null);
        } catch (ConflictException e) {
            log.warn("Bỏ qua job đêm: {}", e.getMessage());
        }
    }
}
