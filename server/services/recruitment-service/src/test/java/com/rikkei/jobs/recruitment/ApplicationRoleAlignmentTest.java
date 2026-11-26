package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.client.CandidateIdentity;
import com.rikkei.jobs.recruitment.client.IdentityCandidateClient;
import com.rikkei.jobs.recruitment.client.JobMatchClient;
import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApplicationRoleAlignmentTest extends ApplicationIntegrationSupport {
    @MockBean IdentityCandidateClient identities;
    @MockBean JobMatchClient matches;
    @org.junit.jupiter.api.BeforeEach void mockReads() {
        when(identities.get(user)).thenReturn(new CandidateIdentity(user, "Applicant", "applicant@example.test"));
        when(identities.getBatch(anyList())).thenReturn(Map.of(user, new CandidateIdentity(user, "Applicant", "applicant@example.test")));
        when(matches.get(any(UUID.class), any(UUID.class), anyString())).thenReturn(Optional.empty());
    }
    @Test void administratorCanReadButCannotChangeApplication() throws Exception {
        UUID id = apply();
        String admin = token(UUID.randomUUID(), Role.ADMIN, null);
        mvc.perform(get("/recruitment/staff/applications").header("Authorization", admin)).andExpect(status().isOk());
        mvc.perform(get("/recruitment/applications/{id}", id).header("Authorization", admin))
            .andExpect(status().isOk());
        verifyNoInteractions(matches);
        mvc.perform(get("/recruitment/applications/{id}/cv", id).header("Authorization", admin))
            .andExpect(status().isOk());
        mvc.perform(post("/recruitment/applications/{id}/transition", id).header("Authorization", admin)
            .contentType("application/json").content("{\"toStatus\":\"SCREENING\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void contactRequiresExplicitAuditedReadAfterApplicationAuthorization() throws Exception {
        UUID id = apply();
        when(contacts.phone(eq(user), anyString())).thenReturn("0912345678");
        mvc.perform(get("/recruitment/applications/{id}", id)
            .header("Authorization", token(UUID.randomUUID(), Role.HR, company)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.candidate.phone").isEmpty())
            .andExpect(jsonPath("$.candidate.email").isEmpty());
        verifyNoInteractions(contacts);
        mvc.perform(get("/candidates/{id}/contact", user)
            .header("Authorization", token(UUID.randomUUID(), Role.HR, company)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("0912345678"));
        clearInvocations(contacts);
        mvc.perform(get("/recruitment/applications/{id}", id)
            .header("Authorization", token(UUID.randomUUID(), Role.HR, UUID.randomUUID())))
            .andExpect(status().isNotFound());
        verifyNoInteractions(contacts);
    }

    @Test void mentorSeesAndScoresOnlyInvitedRound() throws Exception {
        UUID id = apply();
        String hr = token(UUID.randomUUID(), Role.HR, company);
        mvc.perform(post("/recruitment/applications/{id}/transition", id).header("Authorization", hr)
            .contentType("application/json").content("{\"toStatus\":\"SCREENING\"}"))
            .andExpect(status().isOk());
        mvc.perform(post("/recruitment/applications/{id}/transition", id).header("Authorization", hr)
            .contentType("application/json").content("{\"toStatus\":\"INTERVIEW\"}"))
            .andExpect(status().isOk());
        String own = mvc.perform(post("/recruitment/applications/{id}/rounds", id).header("Authorization", hr)
            .contentType("application/json").content("{\"name\":\"Technical\",\"interviewerEmail\":\"mentor@example.test\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID ownRound = UUID.fromString(json.readTree(own).get("id").asText());
        String other = mvc.perform(post("/recruitment/applications/{id}/rounds", id).header("Authorization", hr)
            .contentType("application/json").content("{\"name\":\"Other\",\"interviewerEmail\":\"other@example.test\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID otherRound = UUID.fromString(json.readTree(other).get("id").asText());
        UUID mentorId = UUID.randomUUID();
        when(mentors.email(mentorId)).thenReturn("Mentor@Example.Test");
        String bearer = token(mentorId, Role.MENTOR, null);
        mvc.perform(get("/recruitment/mentor/rounds").header("Authorization", bearer))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].round.id").value(ownRound.toString()))
            .andExpect(jsonPath("$.items[0].candidate").doesNotExist());
        mvc.perform(put("/recruitment/mentor/rounds/{id}", otherRound).header("Authorization", bearer)
            .contentType("application/json").content("{\"result\":\"PASS\",\"score\":90}"))
            .andExpect(status().isNotFound());
        mvc.perform(put("/recruitment/mentor/rounds/{id}", ownRound).header("Authorization", bearer)
            .contentType("application/json").content("{\"result\":\"PASS\",\"score\":85}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.result").value("PASS"));
    }
}
