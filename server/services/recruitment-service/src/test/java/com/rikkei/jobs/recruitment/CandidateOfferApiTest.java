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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CandidateOfferApiTest extends ApplicationIntegrationSupport {
    @Autowired ApplicationTransitionService transitions;
    @Autowired InterviewRoundService rounds;
    @Autowired OfferCommandService offers;

    @Test void onlyOwnerCanReadOrAcceptAndAcceptanceCreatesEmployment() throws Exception {
        OfferResponse offer = readyOffer();
        var outsider = token(UUID.randomUUID(), Role.CANDIDATE, null);
        mvc.perform(get("/recruitment/me/offers/{id}", offer.id()).header("Authorization", outsider))
            .andExpect(status().isNotFound());
        mvc.perform(post("/recruitment/me/offers/{id}/accept", offer.id()).header("Authorization", outsider))
            .andExpect(status().isNotFound());
        mvc.perform(get("/recruitment/me/offers/{id}", offer.id()).header("Authorization", candidate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.salary").value(12000000));
        mvc.perform(post("/recruitment/me/offers/{id}/accept", offer.id()).header("Authorization", candidate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        assertEquals(1, count("employment_records"));
        assertEquals("HIRED", jdbc.queryForObject("select status from recruitment.applications where id=?",
            String.class, offer.applicationId()));
        mvc.perform(post("/recruitment/me/offers/{id}/accept", offer.id()).header("Authorization", candidate()))
            .andExpect(status().isConflict());
        assertEquals(1, count("employment_records"));
    }

    @Test void declineRequiresReasonAndEndsApplication() throws Exception {
        OfferResponse offer = readyOffer();
        mvc.perform(post("/recruitment/me/offers/{id}/decline", offer.id())
            .header("Authorization", candidate()).contentType("application/json").content("{\"reason\":\"  \"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/recruitment/me/offers/{id}/decline", offer.id())
            .header("Authorization", candidate()).contentType("application/json")
            .content("{\"reason\":\"Đã chọn công việc khác\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DECLINED"))
            .andExpect(jsonPath("$.declineReason").value("Đã chọn công việc khác"));
        assertEquals(0, count("employment_records"));
        assertEquals("REJECTED", jdbc.queryForObject("select status from recruitment.applications where id=?",
            String.class, offer.applicationId()));
    }

    @Test void deadlineIsStrictEvenBeforeHourlyJob() throws Exception {
        OfferResponse offer = readyOffer();
        when(clock.instant()).thenReturn(offer.responseDeadline());
        mvc.perform(post("/recruitment/me/offers/{id}/accept", offer.id()).header("Authorization", candidate()))
            .andExpect(status().isConflict());
        mvc.perform(post("/recruitment/me/offers/{id}/decline", offer.id())
            .header("Authorization", candidate()).contentType("application/json")
            .content("{\"reason\":\"Muộn\"}"))
            .andExpect(status().isConflict());
        assertEquals(0, count("employment_records"));
    }

    @Test void concurrentAcceptanceCreatesOnlyOneEmploymentRecord() throws Exception {
        OfferResponse offer = readyOffer();
        var actor = new AuthPrincipal(user, Role.CANDIDATE, null);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> { start.await(); return attemptAccept(actor, offer.id()); });
            var second = pool.submit(() -> { start.await(); return attemptAccept(actor, offer.id()); });
            start.countDown();
            assertEquals(1, (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                + (second.get(10, TimeUnit.SECONDS) ? 1 : 0));
        }
        assertEquals(1, count("employment_records"));
    }

    private boolean attemptAccept(AuthPrincipal actor, UUID offerId) {
        try { offers.accept(actor, offerId); return true; }
        catch (com.rikkei.jobs.common.config.exception.ConflictException expected) { return false; }
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
