package com.rikkei.jobs.profile.service.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.profile.client.ExternalSourceClient;
import com.rikkei.jobs.profile.client.ExternalSourceException;
import com.rikkei.jobs.profile.config.SyncProperties;
import com.rikkei.jobs.profile.entity.SyncRun;
import com.rikkei.jobs.profile.entity.SyncStatus;
import com.rikkei.jobs.profile.entity.SyncTrigger;
import com.rikkei.jobs.profile.repository.SyncRunRepository;
import com.rikkei.jobs.profile.service.sync.CandidateDirectory.Candidate;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

/**
 * Điều phối đồng bộ NT-05. Lấy đủ cả ba nguồn trước rồi mới ghi; một nguồn lỗi thì lần chạy FAILED và dữ liệu cũ
 * giữ nguyên. Mỗi lần chạy (thành công hay lỗi) ghi một dòng sync_runs.
 */
@Slf4j
@Service
public class SyncService {
    private static final int ERROR_MAX = 1000;

    private final CandidateDirectory directory;
    private final ExternalSourceClient client;
    private final SourceRecordMapper mapper;
    private final SyncApplier applier;
    private final SyncRunRepository runs;
    private final SyncProperties properties;
    private final Clock clock;
    private final TaskExecutor executor;
    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean nightlyRunning = new AtomicBoolean();

    public SyncService(CandidateDirectory directory, ExternalSourceClient client, SourceRecordMapper mapper,
                       SyncApplier applier, SyncRunRepository runs, SyncProperties properties, Clock clock,
                       @Qualifier("syncExecutor") TaskExecutor executor) {
        this.directory = directory;
        this.client = client;
        this.mapper = mapper;
        this.applier = applier;
        this.runs = runs;
        this.properties = properties;
        this.clock = clock;
        this.executor = executor;
    }

    /** Đồng bộ ngay một ứng viên (nút "Đồng bộ lại" của cán bộ). */
    public SyncRun syncNow(UUID userId, SyncTrigger trigger, UUID triggeredBy) {
        Candidate candidate = directory.find(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ứng viên"));
        if (candidate.lmsUserId() == null) {
            throw new BadRequestException("Ứng viên chưa liên kết tài khoản LMS nên không có dữ liệu để đồng bộ");
        }
        return run(candidate, trigger, triggeredBy);
    }

    /**
     * Ứng viên mở hồ sơ: lần đồng bộ cuối (kể cả lỗi) cũ hơn {@code app.sync.stale-after} thì đồng bộ ở luồng nền.
     * Không bao giờ ném lỗi ra ngoài để không chặn trang hồ sơ.
     */
    public void syncInBackgroundIfStale(UUID userId) {
        if (!properties.isOnOpenEnabled() || userId == null) {
            return;
        }
        try {
            Instant threshold = now().minus(properties.getStaleAfter());
            boolean fresh = runs.findFirstByUserIdOrderByStartedAtDesc(userId)
                    .map(last -> last.getStartedAt().isAfter(threshold)).orElse(false);
            if (fresh || !inFlight.add(userId)) {
                return;
            }
        } catch (RuntimeException e) {
            log.warn("Không kiểm tra được lần đồng bộ cuối của {}: {}", userId, e.getMessage());
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    directory.find(userId).filter(c -> c.lmsUserId() != null)
                            .ifPresent(c -> run(c, SyncTrigger.ON_OPEN, null));
                } catch (RuntimeException e) {
                    log.warn("Đồng bộ nền cho {} lỗi: {}", userId, e.getMessage());
                } finally {
                    inFlight.remove(userId);
                }
            });
        } catch (TaskRejectedException e) {
            inFlight.remove(userId);
            log.warn("Hàng đợi đồng bộ đầy, bỏ qua đồng bộ nền cho {}", userId);
        }
    }

    /** Job đêm: đồng bộ lần lượt mọi ứng viên đã liên kết LMS. */
    public List<SyncRun> runNightly(UUID triggeredBy) {
        if (!nightlyRunning.compareAndSet(false, true)) {
            throw new ConflictException("Job đồng bộ toàn bộ ứng viên đang chạy");
        }
        try {
            return nightly(triggeredBy);
        } finally {
            nightlyRunning.set(false);
        }
    }

    /** Chạy tay job đêm từ API: trả về ngay, job chạy ở luồng nền. */
    public void startNightlyInBackground(UUID triggeredBy) {
        if (!nightlyRunning.compareAndSet(false, true)) {
            throw new ConflictException("Job đồng bộ toàn bộ ứng viên đang chạy");
        }
        try {
            executor.execute(() -> {
                try {
                    nightly(triggeredBy);
                } catch (RuntimeException e) {
                    log.error("Job đồng bộ toàn bộ ứng viên lỗi", e);
                } finally {
                    nightlyRunning.set(false);
                }
            });
        } catch (TaskRejectedException e) {
            nightlyRunning.set(false);
            throw new ConflictException("Hàng đợi đồng bộ đang đầy, thử lại sau");
        }
    }

    private List<SyncRun> nightly(UUID triggeredBy) {
        Instant started = now();
        List<Candidate> candidates;
        try {
            candidates = directory.syncable();
        } catch (RuntimeException e) {
            log.error("Không đọc được danh sách ứng viên cho job đêm", e);
            return List.of(save(SyncTrigger.NIGHTLY, null, triggeredBy, started, SyncStatus.FAILED,
                    "Không đọc được danh sách ứng viên", null));
        }
        log.info("Job đồng bộ bắt đầu cho {} ứng viên", candidates.size());
        List<SyncRun> result = new ArrayList<>();
        for (Candidate candidate : candidates) {
            result.add(run(candidate, SyncTrigger.NIGHTLY, triggeredBy));
        }
        long failed = result.stream().filter(r -> r.getStatus() == SyncStatus.FAILED).count();
        log.info("Job đồng bộ xong: {} ứng viên, {} lỗi", result.size(), failed);
        return result;
    }

    private SyncRun run(Candidate candidate, SyncTrigger trigger, UUID triggeredBy) {
        Instant started = now();
        try {
            List<JsonNode> lms = client.lmsResults(candidate.lmsUserId());
            List<JsonNode> exam = client.examResults(candidate.lmsUserId());
            List<JsonNode> pm = client.internships(candidate.lmsUserId());
            SourceRecordMapper.Mapped mapped = mapper.map(candidate.lmsUserId(), lms, exam, pm);
            SyncCounts counts = applier.apply(candidate.userId(), mapped.records(),
                    lms.size() + exam.size() + pm.size(), mapped.skipped(), now());
            return save(trigger, candidate.userId(), triggeredBy, started, SyncStatus.SUCCESS, null, counts);
        } catch (ExternalSourceException e) {
            log.warn("Đồng bộ {} lỗi: {}", candidate.lmsUserId(), e.getMessage());
            return save(trigger, candidate.userId(), triggeredBy, started, SyncStatus.FAILED, e.getMessage(), null);
        } catch (RuntimeException e) {
            log.error("Đồng bộ {} lỗi khi ghi dữ liệu", candidate.lmsUserId(), e);
            return save(trigger, candidate.userId(), triggeredBy, started, SyncStatus.FAILED,
                    "Lỗi khi ghi dữ liệu đồng bộ; dữ liệu cũ được giữ nguyên", null);
        }
    }

    private SyncRun save(SyncTrigger trigger, UUID userId, UUID triggeredBy, Instant started, SyncStatus status,
                         String error, SyncCounts counts) {
        SyncRun run = new SyncRun();
        run.setTrigger(trigger);
        run.setUserId(userId);
        run.setTriggeredBy(triggeredBy);
        run.setStatus(status);
        run.setStartedAt(started);
        run.setFinishedAt(now());
        run.setError(error == null || error.length() <= ERROR_MAX ? error : error.substring(0, ERROR_MAX));
        run.setCounts(counts == null ? null : counts.toMap());
        return runs.save(run);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
