package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.client.CandidateIdentity;
import com.rikkei.jobs.recruitment.client.IdentityCandidateClient;
import com.rikkei.jobs.recruitment.client.JobMatchClient;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OfferReadApiTest extends ApplicationIntegrationSupport {
    @Autowired ApplicationTransitionService transitions;
    @Autowired InterviewRoundService rounds;
    @Autowired OfferCommandService offers;
    @MockBean IdentityCandidateClient identities;
    @MockBean JobMatchClient matches;

    @Test void hrSeesOfferButStaffDoesNotAndCandidateExperienceHasNoSalary() throws Exception {
        OfferResponse offer = readyOffer();
        when(identities.get(user)).thenReturn(new CandidateIdentity(user, "Ứng viên A", "a@example.test"));
        when(matches.get(eq(job), eq(user), anyString())).thenReturn(Optional.empty());
        mvc.perform(get("/recruitment/applications/{id}", offer.applicationId())
            .header("Authorization", token(UUID.randomUUID(), Role.HR, company)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.offer.salary").value(12000000))
            .andExpect(jsonPath("$.offer.status").value("SENT"));
        mvc.perform(get("/recruitment/applications/{id}", offer.applicationId())
            .header("Authorization", token(UUID.randomUUID(), Role.STAFF, null)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.offer").doesNotExist());
        mvc.perform(get("/recruitment/me/employment-records").header("Authorization", candidate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
        offers.accept(new AuthPrincipal(user, Role.CANDIDATE, null), offer.id());
        mvc.perform(get("/recruitment/me/employment-records").header("Authorization", candidate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].position").value("Java Developer"))
            .andExpect(jsonPath("$[0].companyName").value("Company A"))
            .andExpect(jsonPath("$[0].salary").doesNotExist());
    }

    @Test void hrSeesDeclineReason() throws Exception {
        OfferResponse offer = readyOffer();
        offers.decline(new AuthPrincipal(user, Role.CANDIDATE, null), offer.id(), "Đã nhận việc khác");
        when(identities.get(user)).thenReturn(new CandidateIdentity(user, "Ứng viên A", "a@example.test"));
        when(matches.get(eq(job), eq(user), anyString())).thenReturn(Optional.empty());
        mvc.perform(get("/recruitment/applications/{id}", offer.applicationId())
            .header("Authorization", token(UUID.randomUUID(), Role.HR, company)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.offer.status").value("DECLINED"))
            .andExpect(jsonPath("$.offer.declineReason").value("Đã nhận việc khác"));
    }

    @Test void hrSeesPersistedOnboardConfirmationAfterReload() throws Exception {
        OfferResponse offer = readyOffer();
        offers.accept(new AuthPrincipal(user, Role.CANDIDATE, null), offer.id());
        offers.confirmOnboard(new AuthPrincipal(UUID.randomUUID(), Role.HR, company), offer.applicationId());
        when(identities.get(user)).thenReturn(new CandidateIdentity(user, "Ứng viên A", "a@example.test"));
        when(matches.get(eq(job), eq(user), anyString())).thenReturn(Optional.empty());
        mvc.perform(get("/recruitment/applications/{id}", offer.applicationId())
            .header("Authorization", token(UUID.randomUUID(), Role.HR, company)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onboardConfirmedAt").value("2030-01-01T00:00:00Z"));
        mvc.perform(get("/recruitment/applications/{id}", offer.applicationId())
            .header("Authorization", token(UUID.randomUUID(), Role.STAFF, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.onboardConfirmedAt").doesNotExist());
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
