package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.dto.request.CreateRoundRequest;
import com.rikkei.jobs.recruitment.dto.request.UpdateRoundRequest;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.entity.InterviewRoundResult;
import com.rikkei.jobs.recruitment.service.ApplicationTransitionService;
import com.rikkei.jobs.recruitment.service.InterviewRoundService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HrOfferApiTest extends ApplicationIntegrationSupport {
    @Autowired ApplicationTransitionService transitions;
    @Autowired InterviewRoundService rounds;

    @Test void sendRequiresLastPassedRoundAndIsAtomic() throws Exception {
        UUID id = interviewing();
        mvc.perform(post("/recruitment/applications/{id}/offer", id)
            .header("Authorization", hr(company)).contentType("application/json").content(offerBody()))
            .andExpect(status().isConflict());
        assertEquals(0, count("offers"));
        var actor = new AuthPrincipal(UUID.randomUUID(), Role.HR, company);
        var round = rounds.create(actor, id, new CreateRoundRequest("Cuối", "hr@example.test", null));
        mvc.perform(post("/recruitment/applications/{id}/offer", id)
            .header("Authorization", hr(company)).contentType("application/json").content(offerBody()))
            .andExpect(status().isConflict());
        rounds.update(actor, id, round.id(), new UpdateRoundRequest(InterviewRoundResult.PASS, null, null));
        mvc.perform(post("/recruitment/applications/{id}/offer", id)
            .header("Authorization", hr(company)).contentType("application/json").content(offerBody()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("SENT"))
            .andExpect(jsonPath("$.salary").value(12000000));
        assertEquals(1, count("offers"));
        assertEquals("OFFER", jdbc.queryForObject("select status from recruitment.applications where id=?", String.class, id));
        assertEquals(3, count("application_status_notification_outbox"));
        mvc.perform(post("/recruitment/applications/{id}/offer", id)
            .header("Authorization", hr(company)).contentType("application/json").content(offerBody()))
            .andExpect(status().isConflict());
    }

    @Test void sendRejectsWrongCompanyAndInvalidSalaryOrDeadline() throws Exception {
        UUID id = interviewing();
        mvc.perform(post("/recruitment/applications/{id}/offer", id)
            .header("Authorization", hr(UUID.randomUUID())).contentType("application/json").content(offerBody()))
            .andExpect(status().isNotFound());
        var actor = new AuthPrincipal(UUID.randomUUID(), Role.HR, company);
        var round = rounds.create(actor, id, new CreateRoundRequest("Cuối", "hr@example.test", null));
        rounds.update(actor, id, round.id(), new UpdateRoundRequest(InterviewRoundResult.PASS, null, null));
        mvc.perform(post("/recruitment/applications/{id}/offer", id)
            .header("Authorization", hr(company)).contentType("application/json")
            .content(offerBody().replace("12000000", "0"))).andExpect(status().isBadRequest());
        mvc.perform(post("/recruitment/applications/{id}/offer", id)
            .header("Authorization", hr(company)).contentType("application/json")
            .content(offerBody().replace("12000000", "12000000.5"))).andExpect(status().isBadRequest());
        mvc.perform(post("/recruitment/applications/{id}/offer", id)
            .header("Authorization", hr(company)).contentType("application/json")
            .content(offerBody().replace("2030-01-03T00:00:00Z", "2029-12-31T00:00:00Z")))
            .andExpect(status().isBadRequest());
        assertEquals(0, count("offers"));
    }

    UUID interviewing() throws Exception {
        UUID id = apply(), hrId = UUID.randomUUID();
        transitions.transition(id, ApplicationStatus.SCREENING, hrId, null);
        transitions.transition(id, ApplicationStatus.INTERVIEW, hrId, null);
        return id;
    }
    String hr(UUID companyId) { return token(UUID.randomUUID(), Role.HR, companyId); }
    String offerBody() {
        return "{\"position\":\"Java Developer\",\"salary\":12000000,\"startDate\":\"2030-02-01\","
            + "\"workLocation\":\"Hà Nội\",\"responseDeadline\":\"2030-01-03T00:00:00Z\"}";
    }
}
