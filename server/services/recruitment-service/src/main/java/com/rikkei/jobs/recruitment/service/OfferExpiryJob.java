package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.entity.OfferStatus;
import com.rikkei.jobs.recruitment.repository.ApplicationRepository;
import com.rikkei.jobs.recruitment.repository.OfferRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class OfferExpiryJob {
    private static final UUID SYSTEM_ACTOR = new UUID(0, 0);
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ApplicationRepository applications;
    private final OfferRepository offers;
    private final ApplicationTransitionService transitions;
    private final Clock clock;

    public OfferExpiryJob(JdbcTemplate jdbc, PlatformTransactionManager manager,
        ApplicationRepository applications, OfferRepository offers,
        ApplicationTransitionService transitions, Clock clock) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
        this.applications = applications;
        this.offers = offers;
        this.transitions = transitions;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scheduledExpire() { expireDue(); }

    public int expireDue() {
        int total = 0;
        while (true) {
            var due = jdbc.query("""
                select id, application_id from recruitment.offers
                where status='SENT' and response_deadline<=?
                order by response_deadline, id limit 100
                """, (rs, row) -> new Due(rs.getObject("id", UUID.class),
                    rs.getObject("application_id", UUID.class)), Timestamp.from(clock.instant()));
            if (due.isEmpty()) return total;
            int expiredThisBatch = 0;
            for (var item : due) {
                Boolean expired = transactions.execute(status -> expireOne(item));
                if (Boolean.TRUE.equals(expired)) { total++; expiredThisBatch++; }
            }
            if (due.size() < 100 || expiredThisBatch == 0) return total;
        }
    }

    private boolean expireOne(Due due) {
        var application = applications.findForUpdate(due.applicationId()).orElse(null);
        if (application == null) return false;
        var offer = offers.findByApplicationIdForUpdate(application.getId()).orElse(null);
        if (offer == null || !offer.getId().equals(due.id()) || offer.getStatus() != OfferStatus.SENT
            || clock.instant().isBefore(offer.getResponseDeadline())) return false;
        if (application.getStatus() != ApplicationStatus.OFFER) return false;
        offer.setStatus(OfferStatus.EXPIRED);
        offers.saveAndFlush(offer);
        transitions.transition(application.getId(), ApplicationStatus.REJECTED, SYSTEM_ACTOR, null);
        return true;
    }

    private record Due(UUID id, UUID applicationId) {}
}
