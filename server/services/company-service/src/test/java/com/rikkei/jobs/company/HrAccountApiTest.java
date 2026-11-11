package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.client.CreatedUser;
import com.rikkei.jobs.company.client.EmailExistsException;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class HrAccountApiTest extends CompanyApiTestSupport {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ResultActions createHr(String token, UUID companyId, String email) throws Exception {
        return mvc.perform(post("/staff/companies/" + companyId + "/hr-accounts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(Map.of("email", email, "fullName", "HR Ba"))));
    }

    @Test
    void createsHrAndLinksCompany() throws Exception {
        Company company = saveCompany("0101234567", CompanyStatus.ACTIVE);
        UUID uid = UUID.randomUUID();
        when(identity.createHrUser("hr3@rikkeijobs.test", "HR Ba", company.getId()))
                .thenReturn(new CreatedUser(uid, "hr3@rikkeijobs.test", "HR Ba", company.getId()));

        createHr(token(Role.STAFF), company.getId(), "hr3@rikkeijobs.test")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(uid.toString()))
                .andExpect(jsonPath("$.email").value("hr3@rikkeijobs.test"))
                .andExpect(jsonPath("$.companyId").value(company.getId().toString()));

        assertThat(companyHrs.findById(uid)).get().extracting("companyId").isEqualTo(company.getId());
    }

    @Test
    void emailIsNormalizedBeforeCallingIdentity() throws Exception {
        Company company = saveCompany("0101234567", CompanyStatus.ACTIVE);
        UUID uid = UUID.randomUUID();
        when(identity.createHrUser(any(), any(), any()))
                .thenReturn(new CreatedUser(uid, "hr3@rikkeijobs.test", "HR Ba", company.getId()));

        createHr(token(Role.STAFF), company.getId(), " HR3@RikkeiJobs.TEST ").andExpect(status().isCreated());

        verify(identity).createHrUser(eq("hr3@rikkeijobs.test"), any(), any());
    }

    @Test
    void duplicateEmailIs409() throws Exception {
        Company company = saveCompany("0101234567", CompanyStatus.ACTIVE);
        when(identity.createHrUser(any(), any(), any()))
                .thenThrow(new EmailExistsException(UUID.randomUUID(), Role.HR, UUID.randomUUID()));

        createHr(token(Role.STAFF), company.getId(), "hr3@rikkeijobs.test")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        assertThat(companyHrs.count()).isZero();
    }

    @Test
    void duplicateEmailOfCandidateIs409() throws Exception {
        Company company = saveCompany("0101234567", CompanyStatus.ACTIVE);
        when(identity.createHrUser(any(), any(), any()))
                .thenThrow(new EmailExistsException(UUID.randomUUID(), Role.CANDIDATE, null));

        createHr(token(Role.STAFF), company.getId(), "hr3@rikkeijobs.test").andExpect(status().isConflict());

        assertThat(companyHrs.count()).isZero();
    }

    @Test
    void retryAfterTimeoutLinksExistingHr() throws Exception {
        Company company = saveCompany("0101234567", CompanyStatus.ACTIVE);
        UUID uid = UUID.randomUUID();
        when(identity.createHrUser(any(), any(), any()))
                .thenThrow(new EmailExistsException(uid, Role.HR, company.getId()));

        createHr(token(Role.STAFF), company.getId(), "hr3@rikkeijobs.test")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(uid.toString()));

        assertThat(companyHrs.existsById(uid)).isTrue();
    }

    @Test
    void retryWhenAlreadyLinkedIs409() throws Exception {
        Company company = saveCompany("0101234567", CompanyStatus.ACTIVE);
        UUID uid = UUID.randomUUID();
        linkHr(uid, company.getId());
        when(identity.createHrUser(any(), any(), any()))
                .thenThrow(new EmailExistsException(uid, Role.HR, company.getId()));

        createHr(token(Role.STAFF), company.getId(), "hr3@rikkeijobs.test").andExpect(status().isConflict());
    }

    @Test
    void unknownCompanyIs404() throws Exception {
        createHr(token(Role.STAFF), UUID.randomUUID(), "hr3@rikkeijobs.test").andExpect(status().isNotFound());

        verifyNoInteractions(identity);
    }

    @Test
    void lockedCompanyIs403CompanyLocked() throws Exception {
        Company company = saveCompany("0101234567", CompanyStatus.LOCKED);

        createHr(token(Role.STAFF), company.getId(), "hr3@rikkeijobs.test")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_LOCKED"));

        verifyNoInteractions(identity);
    }

    @Test
    void identityDownIs503() throws Exception {
        Company company = saveCompany("0101234567", CompanyStatus.ACTIVE);
        when(identity.createHrUser(any(), any(), any()))
                .thenThrow(new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE));

        createHr(token(Role.STAFF), company.getId(), "hr3@rikkeijobs.test")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_UNAVAILABLE"));
    }

    @Test
    void linkFailureDeletesCreatedUser() throws Exception {
        Company company = saveCompany("0101234567", CompanyStatus.ACTIVE);
        UUID uid = UUID.randomUUID();
        Company other = saveCompany("0109999999", CompanyStatus.ACTIVE);
        linkHr(uid, other.getId());
        when(identity.createHrUser(any(), any(), any()))
                .thenReturn(new CreatedUser(uid, "hr3@rikkeijobs.test", "HR Ba", company.getId()));

        createHr(token(Role.STAFF), company.getId(), "hr3@rikkeijobs.test")
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"));

        verify(identity).deleteUser(uid);
    }

    @Test
    void hrTokenIs403() throws Exception {
        Company company = saveCompany("0101234567", CompanyStatus.ACTIVE);

        createHr(token(Role.HR), company.getId(), "hr3@rikkeijobs.test").andExpect(status().isForbidden());
    }
}
