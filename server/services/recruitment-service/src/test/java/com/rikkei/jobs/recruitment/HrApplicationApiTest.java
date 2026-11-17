package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.exception.ConflictException;
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
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HrApplicationApiTest extends ApplicationIntegrationSupport {
    @Autowired ApplicationTransitionService transitions;
    @Autowired InterviewRoundService rounds;

    @Test void hrCannotSkipScreeningAndReceivesStableErrorCode() throws Exception {
        var id = apply();
        mvc.perform(post("/recruitment/applications/{id}/transition", id)
            .header("Authorization", hr(company)).contentType("application/json")
            .content("{\"toStatus\":\"INTERVIEW\",\"note\":\"Bỏ sàng lọc\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_TRANSITION"));
        assertThat(jdbc.queryForObject("select status from recruitment.applications where id=?", String.class, id))
            .isEqualTo("SUBMITTED");
    }

    @Test void hrFromAnotherCompanyCannotTransitionOrDownloadCv() throws Exception {
        var id = apply();
        var outsider = hr(UUID.randomUUID());
        mvc.perform(post("/recruitment/applications/{id}/transition", id)
            .header("Authorization", outsider).contentType("application/json")
            .content("{\"toStatus\":\"SCREENING\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(get("/recruitment/applications/{id}/cv", id).header("Authorization", outsider))
            .andExpect(status().isNotFound());
        mvc.perform(post("/recruitment/applications/{id}/rounds", id)
            .header("Authorization", outsider).contentType("application/json")
            .content("{\"name\":\"Kỹ thuật\",\"interviewerEmail\":\"a@example.test\"}"))
            .andExpect(status().isNotFound());
    }

    @Test void staffCanDownloadCvButCannotWriteWorkflow() throws Exception {
        var id = apply();
        var staff = token(UUID.randomUUID(), Role.STAFF, null);
        mvc.perform(get("/recruitment/applications/{id}/cv", id).header("Authorization", staff))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "private, no-store"))
            .andExpect(content().contentType("application/pdf"));
        mvc.perform(post("/recruitment/applications/{id}/transition", id)
            .header("Authorization", staff).contentType("application/json")
            .content("{\"toStatus\":\"SCREENING\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void hrCanAdvanceAddRoundsAndReviseTheirResults() throws Exception {
        var id = apply();
        mvc.perform(post("/recruitment/applications/{id}/rounds", id)
            .header("Authorization", hr(company)).contentType("application/json")
            .content("{\"name\":\"Kỹ thuật\",\"interviewerEmail\":\"a@example.test\"}"))
            .andExpect(status().isConflict());
        for (var target : new String[]{"SCREENING", "INTERVIEW"}) {
            mvc.perform(post("/recruitment/applications/{id}/transition", id)
                .header("Authorization", hr(company)).contentType("application/json")
                .content("{\"toStatus\":\"" + target + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(target));
        }
        var response = mvc.perform(post("/recruitment/applications/{id}/rounds", id)
            .header("Authorization", hr(company)).contentType("application/json")
            .content("{\"name\":\"Kỹ thuật\",\"interviewerEmail\":\"a@example.test\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.roundNo").value(1))
            .andReturn().getResponse().getContentAsString();
        var roundId = UUID.fromString(json.readTree(response).get("id").asText());
        mvc.perform(put("/recruitment/applications/{id}/rounds/{roundId}", id, roundId)
            .header("Authorization", hr(company)).contentType("application/json")
            .content("{\"result\":\"PASS\",\"score\":85,\"note\":\"Đạt\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result").value("PASS"));
        assertThat(count("interview_round_revisions")).isEqualTo(1);
    }

    @Test void coreOfferTransitionChecksEveryRoundUnderApplicationLock() throws Exception {
        var id = apply();
        var hrId = UUID.randomUUID();
        var actor = new AuthPrincipal(hrId, Role.HR, company);
        transitions.transition(id, ApplicationStatus.SCREENING, hrId, null);
        transitions.transition(id, ApplicationStatus.INTERVIEW, hrId, null);
        assertThatThrownBy(() -> transitions.transition(id, ApplicationStatus.OFFER, hrId, null))
            .isInstanceOf(ConflictException.class);
        var round = rounds.create(actor, id, new CreateRoundRequest("Kỹ thuật", "a@example.test", null));
        assertThatThrownBy(() -> transitions.transition(id, ApplicationStatus.OFFER, hrId, null))
            .isInstanceOf(ConflictException.class);
        rounds.update(actor, id, round.id(), new UpdateRoundRequest(InterviewRoundResult.FAIL, null, null));
        assertThatThrownBy(() -> transitions.transition(id, ApplicationStatus.OFFER, hrId, null))
            .isInstanceOf(ConflictException.class);
        rounds.update(actor, id, round.id(), new UpdateRoundRequest(InterviewRoundResult.PASS, null, null));
        transitions.transition(id, ApplicationStatus.OFFER, hrId, null);
        assertThatThrownBy(() -> rounds.update(actor, id, round.id(),
            new UpdateRoundRequest(InterviewRoundResult.FAIL, null, null))).isInstanceOf(ConflictException.class);
    }

    private String hr(UUID companyId) { return token(UUID.randomUUID(), Role.HR, companyId); }
}
