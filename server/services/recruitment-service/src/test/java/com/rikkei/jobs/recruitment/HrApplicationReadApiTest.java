package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.client.CandidateIdentity;
import com.rikkei.jobs.recruitment.client.IdentityCandidateClient;
import com.rikkei.jobs.recruitment.client.JobMatchClient;
import com.rikkei.jobs.recruitment.dto.response.MatchResponse;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.service.ApplicationTransitionService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HrApplicationReadApiTest extends ApplicationIntegrationSupport {
    @MockBean IdentityCandidateClient identities;
    @MockBean JobMatchClient matches;
    @Autowired ApplicationTransitionService transitions;

    @Test void detailAggregatesCurrentCandidateMatchRoundsAndHistory() throws Exception {
        UUID id = apply();
        transitions.transition(id, ApplicationStatus.SCREENING, UUID.randomUUID(), "Xem CV");
        when(identities.get(user)).thenReturn(new CandidateIdentity(user, "Ứng viên A", "a@example.test"));
        when(matches.get(eq(job), eq(user), anyString())).thenReturn(Optional.of(
            new MatchResponse(new BigDecimal("82"), false, java.util.List.of(), java.util.List.of())));
        mvc.perform(get("/recruitment/applications/{id}", id).header("Authorization", token(UUID.randomUUID(), Role.HR, company)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.candidate.fullName").value("Ứng viên A"))
            .andExpect(jsonPath("$.match.score").value(82)).andExpect(jsonPath("$.history[1].note").value("Xem CV"))
            .andExpect(jsonPath("$.allowedTransitions[0]").exists())
            .andExpect(jsonPath("$.cvSnapshotUrl").exists());
    }

    @Test void otherCompanyHrGets404BeforeAnyDependencyCall() throws Exception {
        UUID id = apply();
        mvc.perform(get("/recruitment/applications/{id}", id)
            .header("Authorization", token(UUID.randomUUID(), Role.HR, UUID.randomUUID())))
            .andExpect(status().isNotFound());
        verifyNoInteractions(identities, matches);
    }

    @Test void temporarilyUnavailableMatchIsNullButIdentityFailureDoesNotInventCandidate() throws Exception {
        UUID id = apply();
        when(identities.get(user)).thenReturn(new CandidateIdentity(user, "Ứng viên A", "a@example.test"));
        when(matches.get(eq(job), eq(user), anyString())).thenReturn(Optional.empty());
        String hr = token(UUID.randomUUID(), Role.HR, company);
        mvc.perform(get("/recruitment/applications/{id}", id).header("Authorization", hr))
            .andExpect(status().isOk()).andExpect(jsonPath("$.match").doesNotExist());
        reset(identities);
        when(identities.get(user)).thenThrow(new com.rikkei.jobs.recruitment.config.RecruitmentDependencyException(
            503, "IDENTITY_UNAVAILABLE", "Identity không sẵn sàng"));
        mvc.perform(get("/recruitment/applications/{id}", id).header("Authorization", hr))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("IDENTITY_UNAVAILABLE"));
    }

    @Test void staffCanReadAndFilterButCannotMutate() throws Exception {
        UUID id = apply();
        when(identities.get(user)).thenReturn(new CandidateIdentity(user, "Ứng viên A", "a@example.test"));
        when(identities.getBatch(anyList())).thenReturn(Map.of(user, new CandidateIdentity(user, "Ứng viên A", "a@example.test")));
        String staff = token(UUID.randomUUID(), Role.STAFF, null);
        mvc.perform(get("/recruitment/applications/{id}", id).header("Authorization", staff))
            .andExpect(status().isOk()).andExpect(jsonPath("$.allowedTransitions").isEmpty());
        mvc.perform(get("/recruitment/staff/applications").param("companyId", company.toString())
            .param("status", "SUBMITTED").header("Authorization", staff))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].candidateName").value("Ứng viên A"));
        mvc.perform(post("/recruitment/applications/{id}/transition", id).header("Authorization", staff)
            .contentType("application/json").content("{\"toStatus\":\"SCREENING\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void hrListIsScopedAndUsesOneBatchLookupForPage() throws Exception {
        apply();
        when(identities.getBatch(anyList())).thenReturn(Map.of(user, new CandidateIdentity(user, "Ứng viên A", "a@example.test")));
        String hr = token(UUID.randomUUID(), Role.HR, company);
        mvc.perform(get("/recruitment/hr/applications").header("Authorization", hr))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/recruitment/hr/applications").param("jobId", UUID.randomUUID().toString())
            .header("Authorization", hr)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        verify(identities, times(1)).getBatch(argThat(ids -> ids.size() == 1 && ids.contains(user)));
        mvc.perform(get("/recruitment/hr/applications").param("page", "0").header("Authorization", hr))
            .andExpect(status().isBadRequest());
    }
}
