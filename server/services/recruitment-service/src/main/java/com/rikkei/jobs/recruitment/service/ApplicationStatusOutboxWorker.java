package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.recruitment.client.IdentityCandidateClient;
import com.rikkei.jobs.recruitment.repository.ApplicationRepository;
import com.rikkei.jobs.recruitment.repository.ApplicationStatusHistoryRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component @Slf4j
public class ApplicationStatusOutboxWorker {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ApplicationRepository applications;
    private final ApplicationStatusHistoryRepository histories;
    private final IdentityCandidateClient identities;
    private final ApplicationStatusMailSender sender;
    private final Clock clock;
    private final boolean enabled;
    private record Claimed(UUID id, UUID applicationId, UUID historyId, UUID userId,
        int attempt, Instant leaseUntil) {}

    public ApplicationStatusOutboxWorker(JdbcTemplate jdbc, PlatformTransactionManager manager,
        ApplicationRepository applications, ApplicationStatusHistoryRepository histories,
        IdentityCandidateClient identities, ApplicationStatusMailSender sender, Clock clock,
        @Value("${app.recruitment.outbox-enabled:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
        this.applications = applications;
        this.histories = histories;
        this.identities = identities;
        this.sender = sender;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${app.recruitment.outbox-delay-ms:10000}")
    public void scheduledDispatch() { if (enabled) dispatchBatch(); }

    public int dispatchBatch() {
        int processed = 0;
        for (int i = 0; i < 20; i++) {
            var row = claim();
            if (row == null) break;
            try {
                var application = applications.findById(row.applicationId()).orElseThrow();
                var history = histories.findById(row.historyId()).orElseThrow();
                var candidate = identities.get(row.userId());
                sender.send(row.id(), candidate.email(), history, application);
                jdbc.update("""
                    update recruitment.application_status_notification_outbox
                    set status='SENT', sent_at=?, lease_until=null
                    where id=? and status='PROCESSING' and attempts=? and lease_until=?
                    """, Timestamp.from(clock.instant()), row.id(), row.attempt(), Timestamp.from(row.leaseUntil()));
            } catch (RuntimeException ex) {
                int[] waits = {60, 300, 900, 3600};
                boolean failed = row.attempt() >= 5;
                var next = clock.instant().plusSeconds(failed ? 0 : waits[row.attempt() - 1]);
                jdbc.update("""
                    update recruitment.application_status_notification_outbox
                    set status=?, next_attempt_at=?, lease_until=null
                    where id=? and status='PROCESSING' and attempts=? and lease_until=?
                    """, failed ? "FAILED" : "PENDING", Timestamp.from(next), row.id(),
                    row.attempt(), Timestamp.from(row.leaseUntil()));
                log.warn("Status notification {} attempt {} failed; status {}", row.id(), row.attempt(),
                    failed ? "FAILED" : "PENDING");
            }
            processed++;
        }
        return processed;
    }

    private Claimed claim() {
        return transactions.execute(status -> {
            var now = clock.instant();
            jdbc.update("""
                update recruitment.application_status_notification_outbox
                set status='FAILED', lease_until=null
                where status='PROCESSING' and lease_until<=? and attempts>=5
                """, Timestamp.from(now));
            var rows = jdbc.query("""
                select id, application_id, status_history_id, user_id, attempts
                from recruitment.application_status_notification_outbox
                where (status='PENDING' and next_attempt_at<=?)
                   or (status='PROCESSING' and lease_until<=? and attempts<5)
                order by created_at, id limit 1 for update skip locked
                """, (rs, index) -> new Claimed(rs.getObject("id", UUID.class),
                    rs.getObject("application_id", UUID.class), rs.getObject("status_history_id", UUID.class),
                    rs.getObject("user_id", UUID.class), rs.getInt("attempts") + 1, now.plusSeconds(60)),
                Timestamp.from(now), Timestamp.from(now));
            if (rows.isEmpty()) return null;
            var row = rows.getFirst();
            jdbc.update("""
                update recruitment.application_status_notification_outbox
                set status='PROCESSING', attempts=?, lease_until=? where id=?
                """, row.attempt(), Timestamp.from(row.leaseUntil()), row.id());
            return row;
        });
    }
}
