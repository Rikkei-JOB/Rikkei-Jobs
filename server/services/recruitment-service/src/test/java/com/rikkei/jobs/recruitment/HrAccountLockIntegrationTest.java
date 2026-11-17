package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.client.HrAccountStatusClient;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HrAccountLockIntegrationTest extends ApplicationIntegrationSupport {
    @Test void lockedHrCannotReadApplicationOrDownloadCvWithOldJwt() throws Exception {
        UUID id = apply();
        when(hrAccounts.state(any(UUID.class), eq(company))).thenReturn(HrAccountStatusClient.State.LOCKED);
        String hr = token(UUID.randomUUID(), Role.HR, company);
        mvc.perform(get("/recruitment/applications/{id}", id).header("Authorization", hr))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        mvc.perform(get("/recruitment/applications/{id}/cv", id).header("Authorization", hr))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        mvc.perform(get("/recruitment/applications/{id}/cv", id)
            .header("Authorization", token(UUID.randomUUID(), Role.STAFF, null))).andExpect(status().isOk());
    }

    @Test void identityFailureFailsClosedForHr() throws Exception {
        UUID id = apply();
        when(hrAccounts.state(any(UUID.class), eq(company))).thenThrow(
            new com.rikkei.jobs.recruitment.config.RecruitmentDependencyException(
                503, "IDENTITY_UNAVAILABLE", "Không xác thực được tài khoản HR."));
        mvc.perform(get("/recruitment/applications/{id}", id)
            .header("Authorization", token(UUID.randomUUID(), Role.HR, company)))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("IDENTITY_UNAVAILABLE"));
    }
}
