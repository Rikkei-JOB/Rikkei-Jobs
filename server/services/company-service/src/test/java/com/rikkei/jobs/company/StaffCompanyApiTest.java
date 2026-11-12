package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class StaffCompanyApiTest extends CompanyApiTestSupport {

    private static final ObjectMapper JSON = new ObjectMapper();

    private ResultActions create(String token, Map<String, Object> body) throws Exception {
        return mvc.perform(post("/staff/companies")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body)));
    }

    private Map<String, Object> body(String taxCode) {
        return Map.of("name", "Công ty Rikkei", "taxCode", taxCode);
    }

    @Test
    void staffCreatesCompany() throws Exception {
        UUID staff = UUID.randomUUID();
        Map<String, Object> body = Map.of(
                "name", "Công ty Rikkei", "taxCode", "0101234567", "industry", "CNTT",
                "size", "51-200", "address", "1 Duy Tân", "city", "Hà Nội",
                "website", "https://rikkei.vn", "description", "Mô tả");

        create(token(staff, Role.STAFF), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.taxCode").value("0101234567"))
                .andExpect(jsonPath("$.id").isNotEmpty());

        Company saved = companies.findAll().get(0);
        assertThat(saved.getCreatedBy()).isEqualTo(staff);
        assertThat(saved.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
    }

    @Test
    void thirteenDigitTaxCodeAccepted() throws Exception {
        create(token(Role.STAFF), body("0101234567001")).andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "12345678901", "123456789012", "12345678901234", "01012345a7", ""})
    void invalidTaxCodeIs400(String taxCode) throws Exception {
        create(token(Role.STAFF), body(taxCode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.taxCode").exists());
    }

    @Test
    void duplicateTaxCodeIs409() throws Exception {
        saveCompany("0101234567", CompanyStatus.ACTIVE);

        create(token(Role.STAFF), body("0101234567"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void nameTooLongIs400() throws Exception {
        create(token(Role.STAFF), Map.of("name", "a".repeat(256), "taxCode", "0101234567"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "ftp://x.vn"})
    void nonHttpWebsiteIs400(String website) throws Exception {
        create(token(Role.STAFF), Map.of("name", "A", "taxCode", "0101234567", "website", website))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.website").exists());
    }

    @Test
    void invalidSizeIs400() throws Exception {
        create(token(Role.STAFF), Map.of("name", "A", "taxCode", "0101234567", "size", "10-20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.size").exists());
    }

    @Test
    void hrCannotCallStaffEndpoints() throws Exception {
        create(token(Role.HR), body("0101234567"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCannotCreateCompany() throws Exception {
        create(token(Role.ADMIN), body("0101234567")).andExpect(status().isForbidden());
    }
}
