package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.internal.service-key=test-internal-key")
class InternalJobContextApiTest extends CompanyApiTestSupport {

    private static final String KEY = "X-Internal-Key";
    private static final String KEY_VALUE = "test-internal-key";

    private static IdentityUserStatus user(UUID id, String email, String status) {
        return new IdentityUserStatus(id, email, "HR", status, null, null, null);
    }

    private Job approvedJob(UUID companyId) {
        Job job = saveJob(companyId, JobStatus.APPROVED);
        job.setDeadline(LocalDate.of(2026, 12, 31));
        return jobs.save(job);
    }

    @Test
    void returnsContractBody() throws Exception {
        Company company = saveCompany("0100000001", CompanyStatus.ACTIVE);
        Job job = approvedJob(company.getId());

        mvc.perform(get("/internal/jobs/{id}/application-context", job.getId()).header(KEY, KEY_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(job.getId().toString()))
                .andExpect(jsonPath("$.companyId").value(company.getId().toString()))
                .andExpect(jsonPath("$.jobTitle").value("JD mẫu"))
                .andExpect(jsonPath("$.companyName").value(company.getName()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.deadline").value("2027-01-01T00:00:00+07:00"))
                .andExpect(jsonPath("$.companyLocked").value(false))
                .andExpect(jsonPath("$.hrEmails").isEmpty());
        verify(identity, never()).lookupUsers(anyList());
    }

    @Test
    @SuppressWarnings("unchecked")
    void hrEmailsSkipLockedAndInactive() throws Exception {
        Company company = saveCompany("0100000002", CompanyStatus.ACTIVE);
        Job job = approvedJob(company.getId());
        UUID hr3 = UUID.randomUUID();
        linkHr(HR1, company.getId());
        linkHr(hr3, company.getId());
        CompanyHr locked = new CompanyHr(HR2, company.getId());
        locked.setLockedAt(Instant.now());
        companyHrs.save(locked);
        when(identity.lookupUsers(anyList())).thenReturn(List.of(
                user(HR1, "HR1@Rikkeijobs.test", "ACTIVE"),
                user(hr3, "hr3@rikkeijobs.test", "LOCKED")));

        mvc.perform(get("/internal/jobs/{id}/application-context", job.getId()).header(KEY, KEY_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hrEmails.length()").value(1))
                .andExpect(jsonPath("$.hrEmails[0]").value("hr1@rikkeijobs.test"));

        ArgumentCaptor<List<UUID>> ids = ArgumentCaptor.forClass(List.class);
        verify(identity).lookupUsers(ids.capture());
        assertThat(ids.getValue()).containsExactlyInAnyOrder(HR1, hr3);
    }

    @Test
    void duplicateEmailsAreCollapsed() throws Exception {
        Company company = saveCompany("0100000007", CompanyStatus.ACTIVE);
        Job job = approvedJob(company.getId());
        UUID other = UUID.randomUUID();
        linkHr(HR1, company.getId());
        linkHr(other, company.getId());
        when(identity.lookupUsers(anyList())).thenReturn(List.of(
                user(HR1, "same@rikkeijobs.test", "ACTIVE"),
                user(other, "SAME@rikkeijobs.test", "ACTIVE")));

        mvc.perform(get("/internal/jobs/{id}/application-context", job.getId()).header(KEY, KEY_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hrEmails.length()").value(1));
    }

    @Test
    void lockedCompanyFlag() throws Exception {
        Company company = saveCompany("0100000003", CompanyStatus.LOCKED);
        Job job = approvedJob(company.getId());

        mvc.perform(get("/internal/jobs/{id}/application-context", job.getId()).header(KEY, KEY_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyLocked").value(true));
    }

    @Test
    void draftWithoutDeadlineUsesCreatedAt() throws Exception {
        Company company = saveCompany("0100000004", CompanyStatus.ACTIVE);
        Job job = saveJob(company.getId(), JobStatus.DRAFT);
        String expected = job.getCreatedAt().atZone(JobRules.ZONE).toOffsetDateTime().toString();

        mvc.perform(get("/internal/jobs/{id}/application-context", job.getId()).header(KEY, KEY_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.deadline").value(endsWith("+07:00")))
                .andExpect(jsonPath("$.deadline").value(startsWith(expected.substring(0, 19))));
    }

    @Test
    void missingOrWrongKeyIs401() throws Exception {
        Company company = saveCompany("0100000005", CompanyStatus.ACTIVE);
        Job job = approvedJob(company.getId());
        String path = "/internal/jobs/" + job.getId() + "/application-context";

        mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_KEY"));
        mvc.perform(get(path).header(KEY, "sai-khoa"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_KEY"));
        mvc.perform(get(path).header("Authorization", "Bearer " + token(HR1, Role.HR)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_KEY"));
    }

    @Test
    void unknownJobIs404() throws Exception {
        mvc.perform(get("/internal/jobs/{id}/application-context", UUID.randomUUID()).header(KEY, KEY_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    void malformedIdIs400() throws Exception {
        mvc.perform(get("/internal/jobs/khong-phai-uuid/application-context").header(KEY, KEY_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    void identityDownIs503() throws Exception {
        Company company = saveCompany("0100000006", CompanyStatus.ACTIVE);
        Job job = approvedJob(company.getId());
        linkHr(HR1, company.getId());
        when(identity.lookupUsers(anyList()))
                .thenThrow(new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE));

        mvc.perform(get("/internal/jobs/{id}/application-context", job.getId()).header(KEY, KEY_VALUE))
                .andExpect(status().isServiceUnavailable());
    }
}
