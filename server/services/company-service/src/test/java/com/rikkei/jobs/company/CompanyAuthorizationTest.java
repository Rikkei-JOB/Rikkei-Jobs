package com.rikkei.jobs.company;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tiêu chí nghiệm thu NT-02 trên API thật {@code GET /companies/{id}}: token candidate gọi API của HR → 403;
 * hr1 (Công ty A) đọc Công ty B → 404, giống hệt khi id không tồn tại. HR thuộc công ty nào lấy từ bảng
 * company_hr (DN-01).
 */
class CompanyAuthorizationTest extends CompanyApiTestSupport {

    UUID companyA;
    UUID companyB;

    @BeforeEach
    void companiesAndHrLinks() {
        companyA = saveCompany("0101234567", CompanyStatus.ACTIVE).getId();
        companyB = saveCompany("0109876543", CompanyStatus.ACTIVE).getId();
        companyHrs.save(new CompanyHr(HR1, companyA));
        companyHrs.save(new CompanyHr(HR2, companyB));
    }

    ResultActions getCompany(UUID id, String token) throws Exception {
        return mvc.perform(get("/companies/" + id).header("Authorization", "Bearer " + token));
    }

    @Test
    void hrReadsOwnCompany() throws Exception {
        getCompany(companyA, token(HR1, Role.HR)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(companyA.toString()));
    }

    @Test
    void hrReadingAnotherCompanyIs404LikeAMissingOne() throws Exception {
        String otherCompany = getCompany(companyB, token(HR1, Role.HR))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        // Không lộ Công ty B có tồn tại hay không: body giống hệt khi id không có thật.
        getCompany(UUID.randomUUID(), token(HR1, Role.HR))
                .andExpect(status().isNotFound())
                .andExpect(content().json(otherCompany, true));
    }

    @Test
    void hrWithoutCompanyLinkReadsNothing() throws Exception {
        getCompany(companyA, token(UUID.randomUUID(), Role.HR)).andExpect(status().isNotFound());
    }

    @Test
    void candidateCallingHrApiIs403() throws Exception {
        getCompany(companyA, token(Role.CANDIDATE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void mentorCallingHrApiIs403() throws Exception {
        getCompany(companyA, token(Role.MENTOR)).andExpect(status().isForbidden());
    }

    @Test
    void staffAndAdminReadAnyCompany() throws Exception {
        getCompany(companyB, token(Role.STAFF)).andExpect(status().isOk());
        getCompany(companyA, token(Role.ADMIN)).andExpect(status().isOk());
    }

    @Test
    void staffReadingMissingCompanyIs404() throws Exception {
        getCompany(UUID.randomUUID(), token(Role.STAFF)).andExpect(status().isNotFound());
    }

    @Test
    void hrOwnCompanyEndpointStillWins() throws Exception {
        // /companies/me (DN-01) không bị /companies/{id} nuốt mất.
        mvc.perform(get("/companies/me").header("Authorization", "Bearer " + token(HR1, Role.HR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(companyA.toString()));
    }

    @Test
    void withoutTokenIs401() throws Exception {
        mvc.perform(get("/companies/" + companyA)).andExpect(status().isUnauthorized());
    }

    @Test
    void apiDocsArePublicAndListTheEndpoint() throws Exception {
        mvc.perform(get("/company/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("/api/v1"))
                .andExpect(jsonPath("$.paths['/companies/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/companies/me'].get").exists());
    }
}
