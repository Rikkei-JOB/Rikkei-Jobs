package com.rikkei.jobs.company;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class HrLockedFilterTest extends CompanyApiTestSupport {

    private Company company;

    @BeforeEach
    void setUp() {
        company = saveCompany("2222222222", CompanyStatus.ACTIVE);
        linkHr(HR1, company.getId());
        linkHr(HR2, company.getId());
        setLockedAt(HR2, Instant.parse("2026-11-17T03:00:00Z"));
    }

    private void setLockedAt(UUID userId, Instant lockedAt) {
        CompanyHr hr = companyHrs.findById(userId).orElseThrow();
        hr.setLockedAt(lockedAt);
        companyHrs.save(hr);
    }

    private static byte[] tinyPng() {
        byte[] b = new byte[64];
        byte[] sig = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(sig, 0, b, 0, sig.length);
        Arrays.fill(b, sig.length, b.length, (byte) 7);
        return b;
    }

    private String bearer(UUID userId, Role role) {
        return "Bearer " + token(userId, role);
    }

    @Test
    void lockedHrIsRejected() throws Exception {
        mvc.perform(get("/companies/me").header("Authorization", bearer(HR2, Role.HR)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"))
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khoá"));
        mvc.perform(get("/companies/me").header("Authorization", bearer(HR1, Role.HR)))
                .andExpect(status().isOk());
    }

    @Test
    void lockedHrIsRejectedOnEveryHrEndpoint() throws Exception {
        String auth = bearer(HR2, Role.HR);
        mvc.perform(get("/companies/" + company.getId()).header("Authorization", auth))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/companies/me").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(multipart("/companies/me/logo")
                        .file(new MockMultipartFile("file", "a.png", "image/png", tinyPng()))
                        .header("Authorization", auth))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/companies/logos/khong-ton-tai.png"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unlockedHrPassesAgain() throws Exception {
        setLockedAt(HR2, null);

        mvc.perform(get("/companies/me").header("Authorization", bearer(HR2, Role.HR)))
                .andExpect(status().isOk());
    }

    @Test
    void hrOfLockedCompanyRejected() throws Exception {
        company.setStatus(CompanyStatus.LOCKED);
        companies.save(company);
        String auth = bearer(HR1, Role.HR);

        mvc.perform(get("/companies/me").header("Authorization", auth))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"))
                .andExpect(jsonPath("$.message").value("Công ty của bạn đang bị khoá, liên hệ cán bộ tuyển dụng"));
        mvc.perform(get("/companies/me/jobs").header("Authorization", auth))
                .andExpect(status().isUnauthorized());
        // Bộ lọc chạy trước cả endpoint công khai: token HR này nhận 401 ở GET /public/jobs (chấp nhận theo spec 4.7).
        mvc.perform(get("/public/jobs").header("Authorization", auth))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/public/jobs")).andExpect(status().isOk());
        // HR vừa khoá riêng vừa thuộc công ty bị khoá: message khoá riêng.
        mvc.perform(get("/companies/me").header("Authorization", bearer(HR2, Role.HR)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khoá"));

        company.setStatus(CompanyStatus.ACTIVE);
        companies.save(company);
        mvc.perform(get("/companies/me").header("Authorization", auth)).andExpect(status().isOk());
    }

    @Test
    void staffNotAffected() throws Exception {
        mvc.perform(get("/companies/" + company.getId())
                        .header("Authorization", bearer(UUID.randomUUID(), Role.STAFF)))
                .andExpect(status().isOk());
    }
}
