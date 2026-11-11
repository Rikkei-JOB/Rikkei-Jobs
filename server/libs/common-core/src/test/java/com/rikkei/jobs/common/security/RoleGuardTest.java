package com.rikkei.jobs.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.access.OwnershipGuard;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.common.config.security.principle.Roles;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Guard dùng chung của common-core, kiểm trên một controller mẫu giống controller thật của các service:
 * mỗi vai trò có ít nhất một ca được phép và một ca bị chặn (403), cộng kiểm sở hữu trả 404.
 */
@SpringBootTest(classes = RoleGuardTest.TestApp.class, properties = "app.jwt.secret=test_secret_test_secret_test_secret_test_secret")
@AutoConfigureMockMvc
class RoleGuardTest {

    static final UUID COMPANY_A = UUID.randomUUID();
    static final UUID COMPANY_B = UUID.randomUUID();
    static final UUID CANDIDATE_1 = UUID.randomUUID();
    static final UUID CANDIDATE_2 = UUID.randomUUID();

    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;

    ResultActions call(String path, Role role, UUID userId, UUID companyId) throws Exception {
        String token = jwt.generateJwtToken(userId, role, companyId);
        return mvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    ResultActions call(String path, Role role) throws Exception {
        return call(path, role, UUID.randomUUID(), role == Role.HR ? COMPANY_A : null);
    }

    @Nested
    class Candidate {
        @Test
        void allowedOnCandidateApi() throws Exception {
            call("/demo/candidate", Role.CANDIDATE).andExpect(status().isOk());
        }

        @Test
        void blockedOnHrApi() throws Exception {
            call("/demo/hr", Role.CANDIDATE)
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                    .andExpect(jsonPath("$.message").value("Bạn không có quyền thực hiện thao tác này."));
        }

        @Test
        void readsOwnDataButOthersIs404() throws Exception {
            call("/demo/users/" + CANDIDATE_1, Role.CANDIDATE, CANDIDATE_1, null).andExpect(status().isOk());
            call("/demo/users/" + CANDIDATE_2, Role.CANDIDATE, CANDIDATE_1, null)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    class Hr {
        @Test
        void allowedOnHrApi() throws Exception {
            call("/demo/hr", Role.HR).andExpect(status().isOk());
        }

        @Test
        void blockedOnStaffApi() throws Exception {
            call("/demo/staff", Role.HR).andExpect(status().isForbidden());
        }

        @Test
        void readsOwnCompanyButOtherCompanyIs404() throws Exception {
            call("/demo/companies/" + COMPANY_A, Role.HR, UUID.randomUUID(), COMPANY_A).andExpect(status().isOk());
            call("/demo/companies/" + COMPANY_B, Role.HR, UUID.randomUUID(), COMPANY_A)
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        void hrWithoutCompanyReadsNothing() throws Exception {
            call("/demo/companies/" + COMPANY_A, Role.HR, UUID.randomUUID(), null).andExpect(status().isNotFound());
        }
    }

    @Nested
    class Mentor {
        @Test
        void allowedOnMentorApi() throws Exception {
            call("/demo/mentor", Role.MENTOR).andExpect(status().isOk());
        }

        @Test
        void blockedOnCandidateApi() throws Exception {
            call("/demo/candidate", Role.MENTOR).andExpect(status().isForbidden());
        }
    }

    @Nested
    class Staff {
        @Test
        void allowedOnStaffApiAndAnyCompany() throws Exception {
            call("/demo/staff", Role.STAFF).andExpect(status().isOk());
            call("/demo/companies/" + COMPANY_B, Role.STAFF).andExpect(status().isOk());
        }

        @Test
        void blockedOnAdminApi() throws Exception {
            call("/demo/admin", Role.STAFF).andExpect(status().isForbidden());
        }
    }

    @Nested
    class Admin {
        @Test
        void allowedOnAdminApiAndAnyUserData() throws Exception {
            call("/demo/admin", Role.ADMIN).andExpect(status().isOk());
            call("/demo/users/" + CANDIDATE_2, Role.ADMIN).andExpect(status().isOk());
        }

        @Test
        void blockedOnCandidateApi() throws Exception {
            call("/demo/candidate", Role.ADMIN).andExpect(status().isForbidden());
        }
    }

    @Test
    void withoutTokenIs401() throws Exception {
        mvc.perform(get("/demo/hr")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    /** Lớp lồng trong lớp test bị bỏ qua khi quét component, nên import controller mẫu trực tiếp. */
    @SpringBootApplication
    @Import(DemoController.class)
    static class TestApp {
    }

    /** Controller mẫu: cách khai báo vai trò và kiểm sở hữu mà các service dùng. */
    @RestController
    static class DemoController {

        @GetMapping("/demo/candidate")
        @PreAuthorize(Roles.CANDIDATE)
        Map<String, String> candidate() {
            return Map.of("ok", "candidate");
        }

        @GetMapping("/demo/hr")
        @PreAuthorize(Roles.HR)
        Map<String, String> hr() {
            return Map.of("ok", "hr");
        }

        @GetMapping("/demo/mentor")
        @PreAuthorize(Roles.MENTOR)
        Map<String, String> mentor() {
            return Map.of("ok", "mentor");
        }

        @GetMapping("/demo/staff")
        @PreAuthorize(Roles.STAFF_OR_ADMIN)
        Map<String, String> staff() {
            return Map.of("ok", "staff");
        }

        @GetMapping("/demo/admin")
        @PreAuthorize(Roles.ADMIN)
        Map<String, String> admin() {
            return Map.of("ok", "admin");
        }

        @GetMapping("/demo/companies/{id}")
        @PreAuthorize(Roles.HR_STAFF_ADMIN)
        Map<String, String> company(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
            OwnershipGuard.requireCompany(principal, id, "Không tìm thấy công ty");
            return Map.of("id", id.toString());
        }

        @GetMapping("/demo/users/{ownerId}")
        @PreAuthorize(Roles.AUTHENTICATED)
        Map<String, String> userData(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID ownerId) {
            OwnershipGuard.requireOwner(principal, ownerId, "Không tìm thấy dữ liệu");
            return Map.of("owner", ownerId.toString());
        }
    }
}
