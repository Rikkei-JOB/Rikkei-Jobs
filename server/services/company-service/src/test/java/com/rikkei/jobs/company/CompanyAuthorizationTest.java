package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.config.DevDataSeeder;
import com.rikkei.jobs.company.repository.CompanyRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tiêu chí nghiệm thu NT-02 trên endpoint thật: token candidate gọi API của HR → 403; hr1 (Công ty A) đọc
 * Công ty B → 404. Chạy Flyway trên DB trống (H2) và seed doanh nghiệp mẫu.
 */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:company-authz;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "app.dev-data.enabled=true",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret"
})
@AutoConfigureMockMvc
class CompanyAuthorizationTest {

    static final UUID A = DevDataSeeder.COMPANY_A_ID;
    static final UUID B = DevDataSeeder.COMPANY_B_ID;

    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired CompanyRepository companies;

    ResultActions getCompany(UUID id, Role role, UUID companyId) throws Exception {
        String token = jwt.generateJwtToken(UUID.randomUUID(), role, companyId);
        return mvc.perform(get("/company/companies/" + id).header("Authorization", "Bearer " + token));
    }

    @Test
    void migrationAndSeedCreateBothSampleCompanies() {
        assertThat(companies.findAllById(java.util.List.of(A, B))).hasSize(2);
    }

    @Test
    void hrReadsOwnCompany() throws Exception {
        getCompany(A, Role.HR, A).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Công ty A"));
    }

    @Test
    void hrReadingAnotherCompanyIs404LikeAMissingOne() throws Exception {
        String otherCompany = getCompany(B, Role.HR, A)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);

        // Không lộ công ty B có tồn tại hay không: body giống hệt khi id không có thật.
        getCompany(UUID.randomUUID(), Role.HR, A).andExpect(status().isNotFound()).andExpect(content().json(otherCompany, true));
    }

    @Test
    void candidateCallingHrApiIs403() throws Exception {
        getCompany(A, Role.CANDIDATE, null).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void mentorCallingHrApiIs403() throws Exception {
        getCompany(A, Role.MENTOR, null).andExpect(status().isForbidden());
    }

    @Test
    void staffAndAdminReadAnyCompany() throws Exception {
        getCompany(B, Role.STAFF, null).andExpect(status().isOk());
        getCompany(A, Role.ADMIN, null).andExpect(status().isOk());
    }

    @Test
    void staffReadingMissingCompanyIs404() throws Exception {
        getCompany(UUID.randomUUID(), Role.STAFF, null).andExpect(status().isNotFound());
    }

    @Test
    void withoutTokenIs401() throws Exception {
        mvc.perform(get("/company/companies/" + A)).andExpect(status().isUnauthorized());
    }

    @Test
    void apiDocsArePublicAndListTheEndpoint() throws Exception {
        mvc.perform(get("/company/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.servers[0].url").value("/api/v1"))
                .andExpect(jsonPath("$.paths['/company/companies/{id}'].get").exists());
    }
}
