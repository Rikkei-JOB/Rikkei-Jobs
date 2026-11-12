package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.recruitment.repository.ApplicationRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component @Slf4j
public class ApplicationOutboxWorker {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ApplicationRepository applications;
    private final ApplicationMailSender sender;
    private final Clock clock;
    private final boolean enabled;
    private record Claimed(UUID id, UUID applicationId, String recipient, int attempt, Instant leaseUntil) {}
    public ApplicationOutboxWorker(JdbcTemplate jdbc, org.springframework.transaction.PlatformTransactionManager manager,
        ApplicationRepository applications, ApplicationMailSender sender, Clock clock,
        @Value("${app.recruitment.outbox-enabled:true}") boolean enabled) {
        this.jdbc = jdbc; this.transactions = new TransactionTemplate(manager); this.applications = applications;
        this.sender = sender; this.clock = clock; this.enabled = enabled;
    }
    @Scheduled(fixedDelayString="${app.recruitment.outbox-delay-ms:10000}")
    public void scheduledDispatch() { if (enabled) dispatchBatch(); }

    public int dispatchBatch() {
        // Claim only one row at a time: its lease must not expire while earlier emails in a batch send.
        int processed = 0;
        for (int i = 0; i < 20; i++) {
            var claimed = claim();
            if (claimed == null) break;
            try {
                var application = applications.findById(claimed.applicationId()).orElseThrow();
                sender.sendNewApplication(claimed.id(), claimed.recipient(), application);
                jdbc.update("update recruitment.application_notification_outbox set status='SENT', sent_at=?, lease_until=null where id=? and status='PROCESSING' and attempts=? and lease_until=?",
                    Timestamp.from(clock.instant()), claimed.id(), claimed.attempt(), Timestamp.from(claimed.leaseUntil()));
            } catch (RuntimeException ex) {
                int[] waits = {60, 300, 900, 3600};
                boolean failed = claimed.attempt() >= 5;
                var next = clock.instant().plusSeconds(failed ? 0 : waits[claimed.attempt() - 1]);
                jdbc.update("update recruitment.application_notification_outbox set status=?, next_attempt_at=?, lease_until=null where id=? and status='PROCESSING' and attempts=? and lease_until=?",
                    failed ? "FAILED" : "PENDING", Timestamp.from(next), claimed.id(), claimed.attempt(), Timestamp.from(claimed.leaseUntil()));
                log.warn("Application notification {} attempt {} failed; status {}", claimed.id(), claimed.attempt(), failed ? "FAILED" : "PENDING");
            }
            processed++;
        }
        return processed;
    }
    private Claimed claim() {
        return transactions.execute(status -> {
            var now = clock.instant();
            // A crashed fifth attempt is terminal after its lease, rather than sending attempt six.
            jdbc.update("update recruitment.application_notification_outbox set status='FAILED', lease_until=null where status='PROCESSING' and lease_until<=? and attempts>=5", Timestamp.from(now));
            var rows = jdbc.query("""
                select id, application_id, recipient, attempts from recruitment.application_notification_outbox
                where (status='PENDING' and next_attempt_at<=?) or (status='PROCESSING' and lease_until<=? and attempts<5)
                order by created_at, id limit 1 for update skip locked
                """, (rs, index) -> new Claimed(rs.getObject("id", UUID.class), rs.getObject("application_id", UUID.class),
                    rs.getString("recipient"), rs.getInt("attempts") + 1, now.plusSeconds(60)), Timestamp.from(now), Timestamp.from(now));
            if (rows.isEmpty()) return null;
            var row = rows.getFirst();
            jdbc.update("update recruitment.application_notification_outbox set status='PROCESSING', attempts=?, lease_until=? where id=?",
                row.attempt(), Timestamp.from(row.leaseUntil()), row.id());
            return row;
        });
    }
}
