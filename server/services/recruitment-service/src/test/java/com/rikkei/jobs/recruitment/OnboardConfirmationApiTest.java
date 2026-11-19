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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OnboardConfirmationApiTest extends ApplicationIntegrationSupport {
    @Autowired ApplicationTransitionService transitions;
    @Autowired InterviewRoundService rounds;
    @Autowired OfferCommandService offers;

    @Test void onlyCompanyHrConfirmsAcceptedOfferAndRepeatKeepsTimestamp() throws Exception {
        OfferResponse offer = readyOffer();
        mvc.perform(post("/recruitment/applications/{id}/offer/confirm-onboard", offer.applicationId())
            .header("Authorization", hr(company))).andExpect(status().isConflict());
        offers.accept(new AuthPrincipal(user, Role.CANDIDATE, null), offer.id());
        mvc.perform(post("/recruitment/applications/{id}/offer/confirm-onboard", offer.applicationId())
            .header("Authorization", hr(UUID.randomUUID()))).andExpect(status().isNotFound());
        mvc.perform(post("/recruitment/applications/{id}/offer/confirm-onboard", offer.applicationId())
            .header("Authorization", hr(company))).andExpect(status().isOk())
            .andExpect(jsonPath("$.confirmedAt").value("2030-01-01T00:00:00Z"));
        mvc.perform(post("/recruitment/applications/{id}/offer/confirm-onboard", offer.applicationId())
            .header("Authorization", hr(company))).andExpect(status().isOk())
            .andExpect(jsonPath("$.confirmedAt").value("2030-01-01T00:00:00Z"));
        assertEquals(1, count("employment_records"));
        assertEquals(4, count("application_status_notification_outbox"));
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
    String hr(UUID companyId) { return token(UUID.randomUUID(), Role.HR, companyId); }
}
