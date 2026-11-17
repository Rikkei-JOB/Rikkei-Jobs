package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class StaffHrAccountLockApiTest extends CompanyApiTestSupport {

    private static final UUID STAFF1 = UUID.fromString("00000000-0000-0000-0001-000000000005");

    @BeforeEach
    void seed() {
        linkHr(HR2, saveCompany("0101234567", CompanyStatus.ACTIVE).getId());
    }

    private ResultActions lock(UUID userId, String body) throws Exception {
        return mvc.perform(post("/staff/hr-accounts/" + userId + "/lock")
                .header("Authorization", "Bearer " + token(STAFF1, Role.STAFF))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions unlock(UUID userId) throws Exception {
        return mvc.perform(post("/staff/hr-accounts/" + userId + "/unlock")
                .header("Authorization", "Bearer " + token(STAFF1, Role.STAFF)));
    }

    private IdentityUserStatus userStatus(String status, Instant lockedAt) {
        return new IdentityUserStatus(HR2, "hr2@x.vn", "HR Hai", status,
                lockedAt == null ? null : "Nghỉ việc", lockedAt, null);
    }

    @Test
    void lockCallsIdentityAndCopiesLock() throws Exception {
        Instant t = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        when(identity.lockUser(HR2, "Nghỉ việc")).thenReturn(userStatus("LOCKED", t));

        lock(HR2, "{\"reason\":\"Nghỉ việc\"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.userId").value(HR2.toString()))
                .andExpect(jsonPath("$.email").value("hr2@x.vn"))
                .andExpect(jsonPath("$.lockedReason").value("Nghỉ việc"));

        assertThat(companyHrs.findById(HR2).orElseThrow().getLockedAt()).isEqualTo(t);
        mvc.perform(get("/companies/me").header("Authorization", "Bearer " + token(HR2, Role.HR)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void blankReasonIs400WithoutCallingIdentity() throws Exception {
        for (String body : new String[] {
                "{\"reason\":\"\"}", "{\"reason\":\"   \"}",
                "{\"reason\":\"" + "a".repeat(501) + "\"}", "{}"}) {
            lock(HR2, body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.reason").exists());
        }
        verifyNoInteractions(identity);
    }

    @Test
    void lockNonHrUserIs404WithoutCallingIdentity() throws Exception {
        lock(STAFF1, "{\"reason\":\"x\"}").andExpect(status().isNotFound());
        lock(UUID.randomUUID(), "{\"reason\":\"x\"}").andExpect(status().isNotFound());
        unlock(UUID.randomUUID()).andExpect(status().isNotFound());
        verifyNoInteractions(identity);
    }

    @Test
    void identityDownKeepsCopyUnchanged() throws Exception {
        when(identity.lockUser(any(), any())).thenThrow(new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE));
        lock(HR2, "{\"reason\":\"x\"}").andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_UNAVAILABLE"));
        assertThat(companyHrs.findById(HR2).orElseThrow().getLockedAt()).isNull();
    }

    @Test
    void unlockClearsCopy() throws Exception {
        CompanyHr hr = companyHrs.findById(HR2).orElseThrow();
        hr.setLockedAt(Instant.now());
        companyHrs.save(hr);
        when(identity.unlockUser(HR2)).thenReturn(userStatus("ACTIVE", null));

        unlock(HR2).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(companyHrs.findById(HR2).orElseThrow().getLockedAt()).isNull();
        mvc.perform(get("/companies/me").header("Authorization", "Bearer " + token(HR2, Role.HR)))
                .andExpect(status().isOk());
    }

    @Test
    void lockTwiceIs200() throws Exception {
        when(identity.lockUser(eq(HR2), any())).thenReturn(userStatus("LOCKED", Instant.now()));
        lock(HR2, "{\"reason\":\"x\"}").andExpect(status().isOk());
        lock(HR2, "{\"reason\":\"x\"}").andExpect(status().isOk());
        verify(identity, times(2)).lockUser(eq(HR2), any());
    }

    @Test
    void hrTokenIs403() throws Exception {
        String hr = "Bearer " + token(HR1, Role.HR);
        mvc.perform(post("/staff/hr-accounts/" + HR2 + "/lock").header("Authorization", hr)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"x\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/staff/hr-accounts/" + HR2 + "/unlock").header("Authorization", hr))
                .andExpect(status().isForbidden());
    }
}
