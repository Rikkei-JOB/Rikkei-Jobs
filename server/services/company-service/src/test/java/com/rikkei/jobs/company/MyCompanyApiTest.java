package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class MyCompanyApiTest extends CompanyApiTestSupport {

    private static final ObjectMapper JSON = new ObjectMapper();

    private Company a;
    private Company b;

    @BeforeEach
    void setUp() {
        a = saveCompany("1111111111", CompanyStatus.ACTIVE);
        b = saveCompany("2222222222", CompanyStatus.ACTIVE);
        linkHr(HR1, a.getId());
        linkHr(HR2, b.getId());
    }

    /** Token của hr1 nhưng claim companyId trỏ sang công ty B: claim phải bị bỏ qua. */
    private String hr1Token() {
        return jwt.generateJwtToken(HR1, Role.HR, b.getId());
    }

    private ResultActions putMe(String token, Map<String, Object> body) throws Exception {
        return mvc.perform(put("/companies/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body)));
    }

    private ResultActions getMe(String token) throws Exception {
        return mvc.perform(get("/companies/me").header("Authorization", "Bearer " + token));
    }

    @Test
    void hrGetsOwnCompanyEvenIfClaimSaysOther() throws Exception {
        getMe(hr1Token()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId().toString()));
    }

    @Test
    void hrUpdatesOwnCompany() throws Exception {
        putMe(hr1Token(), Map.of("name", "Tên mới", "city", "Hà Nội")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tên mới"));
        Company saved = companies.findById(a.getId()).orElseThrow();
        assertThat(saved.getName()).isEqualTo("Tên mới");
        assertThat(saved.getCity()).isEqualTo("Hà Nội");
    }

    @Test
    void bodyIdsAndProtectedFieldsAreIgnored() throws Exception {
        String bName = b.getName();
        putMe(hr1Token(), Map.of("id", b.getId().toString(), "companyId", b.getId().toString(),
                "taxCode", "9999999999", "status", "LOCKED", "name", "Đổi tên"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId().toString()));

        Company savedA = companies.findById(a.getId()).orElseThrow();
        assertThat(savedA.getName()).isEqualTo("Đổi tên");
        assertThat(savedA.getTaxCode()).isEqualTo("1111111111");
        assertThat(savedA.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(companies.findById(b.getId()).orElseThrow().getName()).isEqualTo(bName);
        getMe(token(HR2, Role.HR)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(bName));
    }

    @Test
    void putByIdIsNotAllowed() throws Exception {
        // Không có API sửa công ty theo id; NT-02 thêm GET /companies/{id} nên PUT cùng đường dẫn trả 405
        // (cho mọi id, không lộ id có thật). Điều cần giữ: HR không sửa được công ty khác.
        mvc.perform(put("/companies/" + b.getId())
                        .header("Authorization", "Bearer " + hr1Token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON.writeValueAsString(Map.of("name", "Hack"))))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        assertThat(companies.findById(b.getId()).orElseThrow().getName()).isNotEqualTo("Hack");
    }

    @Test
    void hrWithoutCompanyIs404() throws Exception {
        getMe(token(UUID.randomUUID(), Role.HR)).andExpect(status().isNotFound());
    }

    @Test
    void nonHrIs403() throws Exception {
        getMe(token(Role.STAFF)).andExpect(status().isForbidden());
    }

    @Test
    void lockedCompanyCanBeReadButNotUpdated() throws Exception {
        a.setStatus(CompanyStatus.LOCKED);
        companies.save(a);
        getMe(hr1Token()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));
        putMe(hr1Token(), Map.of("name", "Tên mới")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_LOCKED"));
    }

    @Test
    void blankOptionalFieldsBecomeNull() throws Exception {
        putMe(hr1Token(), Map.of("name", "A", "size", "", "website", "", "city", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(nullValue()))
                .andExpect(jsonPath("$.website").value(nullValue()))
                .andExpect(jsonPath("$.city").value(nullValue()));
    }

    @Test
    void fieldTooLongIs400() throws Exception {
        String website = "https://" + "a".repeat(248); // 256 ký tự
        putMe(hr1Token(), Map.of("name", "A", "website", website)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.website").exists());
    }

    @Test
    void mustChangePasswordTokenIs403() throws Exception {
        getMe(jwt.generateJwtToken(HR1, Role.HR, a.getId(), true)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MUST_CHANGE_PASSWORD"));
    }
}
