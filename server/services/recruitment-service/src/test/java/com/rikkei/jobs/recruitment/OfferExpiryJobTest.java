package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.dto.request.CreateOfferRequest;
import com.rikkei.jobs.recruitment.dto.request.CreateRoundRequest;
import com.rikkei.jobs.recruitment.dto.request.UpdateRoundRequest;
import com.rikkei.jobs.recruitment.dto.response.OfferResponse;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.entity.InterviewRoundResult;
import com.rikkei.jobs.recruitment.service.ApplicationTransitionService;
import com.rikkei.jobs.recruitment.service.InterviewRoundService;
import com.rikkei.jobs.recruitment.service.OfferCommandService;
import com.rikkei.jobs.recruitment.service.OfferExpiryJob;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class OfferExpiryJobTest extends ApplicationIntegrationSupport {
    @Autowired ApplicationTransitionService transitions;
    @Autowired InterviewRoundService rounds;
    @Autowired OfferCommandService offers;
    @Autowired OfferExpiryJob expiry;

    @Test void dueOfferExpiresOnceAndRejectsApplication() throws Exception {
        OfferResponse offer = readyOffer();
        when(clock.instant()).thenReturn(offer.responseDeadline());
        assertEquals(1, expiry.expireDue());
        assertEquals("EXPIRED", jdbc.queryForObject("select status from recruitment.offers where id=?", String.class, offer.id()));
        assertEquals("REJECTED", jdbc.queryForObject("select status from recruitment.applications where id=?",
            String.class, offer.applicationId()));
        assertEquals(4, count("application_status_notification_outbox"));
        assertEquals(0, expiry.expireDue());
        assertEquals(4, count("application_status_notification_outbox"));
    }

    @Test void acceptedOfferIsNotExpired() throws Exception {
        OfferResponse offer = readyOffer();
        offers.accept(new AuthPrincipal(user, Role.CANDIDATE, null), offer.id());
        when(clock.instant()).thenReturn(offer.responseDeadline());
        assertEquals(0, expiry.expireDue());
        assertEquals("ACCEPTED", jdbc.queryForObject("select status from recruitment.offers where id=?", String.class, offer.id()));
    }

    OfferResponse readyOffer() throws Exception {
        UUID id = apply(), hrId = UUID.randomUUID();
        var actor = new AuthPrincipal(hrId, Role.HR, company);
        transitions.transition(id, ApplicationStatus.SCREENING, hrId, null);
        transitions.transition(id, ApplicationStatus.INTERVIEW, hrId, null);
        var round = rounds.create(actor, id, new CreateRoundRequest("Cuối", "hr@example.test", null));
        rounds.update(actor, id, round.id(), new UpdateRoundRequest(InterviewRoundResult.PASS, null, null));
        return offers.send(actor, id, new CreateOfferRequest("Java Developer", new BigDecimal("12000000"),
            LocalDate.parse("2030-02-01"), "Hà Nội", OffsetDateTime.parse("2030-01-03T00:00:00Z"), null));
    }
}
