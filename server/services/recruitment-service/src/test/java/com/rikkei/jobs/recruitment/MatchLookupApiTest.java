package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.client.*;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.rikkei.jobs.recruitment.dto.response.MatchResponse;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.hasItem;

class MatchLookupApiTest extends ApplicationIntegrationSupport {
    @MockBean IdentityCandidateClient identities;
    @MockBean JobMatchClient matches;
    UUID applicationId;
    @BeforeEach void fixture()throws Exception {
        applicationId=apply();
        when(identities.get(user)).thenReturn(new CandidateIdentity(user,"Candidate","candidate@test.local"));
        when(matches.get(eq(job),eq(user),anyString())).thenReturn(Optional.of(new MatchResponse(BigDecimal.ZERO,true,List.of(),List.of())));
    }
    String hr(){return token(UUID.randomUUID(),Role.HR,company);}
    @Test void detailKeepsAllCoreDataWhenMatchingFailsButNotWhenIdentityFails()throws Exception {
        when(matches.get(any(),any(),any())).thenThrow(new RecruitmentDependencyException(502,"MATCH_CONTRACT_ERROR","Upstream auth or payload error"));
        mvc.perform(get("/recruitment/applications/{id}",applicationId).header("Authorization",hr()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.matchStatus").value("UNAVAILABLE")).andExpect(jsonPath("$.match").isEmpty())
            .andExpect(jsonPath("$.candidate.fullName").value("Candidate")).andExpect(jsonPath("$.cvSnapshotUrl").exists()).andExpect(jsonPath("$.rounds").isArray()).andExpect(jsonPath("$.history").isArray()).andExpect(jsonPath("$.allowedTransitions",hasItem("SCREENING")));
        when(identities.get(user)).thenThrow(new RecruitmentDependencyException(503,"IDENTITY_UNAVAILABLE","Unavailable"));
        mvc.perform(get("/recruitment/applications/{id}",applicationId).header("Authorization",hr())).andExpect(status().isServiceUnavailable());
    }
    @Test void zeroIsAvailableAndAdminNeverRequestsMatching()throws Exception {
        mvc.perform(get("/recruitment/applications/{id}",applicationId).header("Authorization",hr())).andExpect(status().isOk()).andExpect(jsonPath("$.matchStatus").value("AVAILABLE")).andExpect(jsonPath("$.match.score").value(0));
        clearInvocations(matches);
        mvc.perform(get("/recruitment/applications/{id}",applicationId).header("Authorization",token(UUID.randomUUID(),Role.ADMIN,null))).andExpect(status().isOk()).andExpect(jsonPath("$.matchStatus").value("NOT_REQUESTED")).andExpect(jsonPath("$.match").isEmpty());
        verifyNoInteractions(matches);
    }
    @Test void matchOnlyAuthorizesBeforeLookupAndDoesNotCallIdentity()throws Exception {
        mvc.perform(get("/recruitment/applications/{id}/match",applicationId).header("Authorization",token(UUID.randomUUID(),Role.HR,UUID.randomUUID()))).andExpect(status().isNotFound());
        mvc.perform(get("/recruitment/applications/{id}/match",UUID.randomUUID()).header("Authorization",hr())).andExpect(status().isNotFound());
        verifyNoInteractions(matches,identities);
        for(Role role:new Role[]{Role.HR,Role.STAFF}) {
            String bearer=token(UUID.randomUUID(),role,role==Role.HR?company:null);
            mvc.perform(get("/recruitment/applications/{id}/match",applicationId).header("Authorization",bearer)).andExpect(status().isOk()).andExpect(jsonPath("$.matchStatus").value("AVAILABLE")).andExpect(jsonPath("$.match.score").value(0));
        }
        verifyNoInteractions(identities);
    }
    @Test void retryRetainsRoleAndAuthenticationGuards()throws Exception {
        for(Role role:new Role[]{Role.ADMIN,Role.CANDIDATE,Role.MENTOR})mvc.perform(get("/recruitment/applications/{id}/match",applicationId).header("Authorization",token(UUID.randomUUID(),role,null))).andExpect(status().isForbidden());
        mvc.perform(get("/recruitment/applications/{id}/match",applicationId)).andExpect(status().isUnauthorized());
        verifyNoInteractions(matches);
    }
    @Test void retryCanRecoverWithoutMutatingApplication()throws Exception {
        when(matches.get(any(),any(),any())).thenReturn(Optional.empty());
        mvc.perform(get("/recruitment/applications/{id}/match",applicationId).header("Authorization",hr())).andExpect(status().isOk()).andExpect(jsonPath("$.matchStatus").value("UNAVAILABLE"));
        when(matches.get(any(),any(),any())).thenReturn(Optional.of(new MatchResponse(new BigDecimal("78"),false,List.of(),List.of())));
        mvc.perform(get("/recruitment/applications/{id}/match",applicationId).header("Authorization",hr())).andExpect(status().isOk()).andExpect(jsonPath("$.matchStatus").value("AVAILABLE")).andExpect(jsonPath("$.match.score").value(78));
        org.junit.jupiter.api.Assertions.assertEquals("SUBMITTED",jdbc.queryForObject("select status from recruitment.applications where id=?",String.class,applicationId));
    }
}
