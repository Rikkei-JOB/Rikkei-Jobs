package com.rikkei.jobs.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.client.LmsClient;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.repository.PasswordResetTokenRepository;
import com.rikkei.jobs.identity.repository.UserRepository;
import com.rikkei.jobs.identity.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Quản trị tài khoản: chỉ ADMIN; khoá có hiệu lực ngay; không tự khoá mình hoặc khoá ADMIN khác. */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:identity-admin-users;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "app.dev-data.enabled=false",
            "app.lms.base-url=http://localhost:0",
            "app.mail.enabled=false"
        })
@AutoConfigureMockMvc
class AdminUserApiTest {

    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository resetTokens;
    @MockBean LmsClient lms;
    @MockBean MailService mail;

    User admin;
    User otherAdmin;
    User mentor;
    User candidate;

    @BeforeEach
    void seed() {
        resetTokens.deleteAll();
        users.deleteAll();
        admin = user("admin@test", "Quản Trị", Role.ADMIN, "lms-ad-1");
        otherAdmin = user("admin2@test", "Quản Trị Hai", Role.ADMIN, "lms-ad-2");
        mentor = user("mentor@test", "Phạm Mentor", Role.MENTOR, "lms-mt-1");
        candidate = user("an@test", "Nguyễn Văn An", Role.CANDIDATE, "lms-sv-1");
    }

    private User user(String email, String name, Role role, String lms) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(name);
        u.setRole(role);
        u.setLmsUserId(lms);
        return users.save(u);
    }

    private String bearer(User u) { return "Bearer " + jwt.generateJwtToken(u.getId(), u.getRole(), null); }

    private ResultActions lock(User actor, User target, String reason) throws Exception {
        return mvc.perform(patch("/admin/users/" + target.getId() + "/lock").header("Authorization", bearer(actor))
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"" + reason + "\"}"));
    }

    @Test
    void listFiltersAndSummary() throws Exception {
        mvc.perform(get("/admin/users").header("Authorization", bearer(admin))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4));
        mvc.perform(get("/admin/users").param("role", "MENTOR").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.items[*].email", contains("mentor@test")));
        mvc.perform(get("/admin/users").param("q", "VĂN AN").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.items[*].email", contains("an@test")));
        mvc.perform(get("/admin/users").param("q", "lms-mt").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/admin/users/summary").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.byRole.ADMIN").value(2))
                .andExpect(jsonPath("$.byRole.HR").value(0))
                .andExpect(jsonPath("$.locked").value(0));
        mvc.perform(get("/admin/users").param("status", "WHATEVER").header("Authorization", bearer(admin))).andExpect(status().isBadRequest());
    }

    @Test
    void lockBlocksImmediatelyAndUnlockRestores() throws Exception {
        lock(admin, mentor, "Vi phạm quy chế đánh giá").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.lockedReason").value("Vi phạm quy chế đánh giá"));
        // Token đã cấp của tài khoản bị khoá không dùng được nữa (401 ACCOUNT_DISABLED, giống AdminLockAuthTest).
        mvc.perform(get("/auth/me").header("Authorization", bearer(mentor))).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        mvc.perform(get("/admin/users").param("status", "LOCKED").header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.items[*].email", contains("mentor@test")));
        mvc.perform(patch("/admin/users/" + mentor.getId() + "/unlock").header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        assertThat(users.findById(mentor.getId()).orElseThrow().getLockedReason()).isNull();
        mvc.perform(get("/auth/me").header("Authorization", bearer(mentor))).andExpect(status().isOk());
    }

    @Test
    void guardsAgainstSelfAndAdminLockout() throws Exception {
        lock(admin, admin, "thử").andExpect(status().isConflict());
        lock(admin, otherAdmin, "thử").andExpect(status().isConflict());
        lock(admin, candidate, "   ").andExpect(status().isBadRequest());
    }

    @Test
    void onlyAdmins() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin/users").header("Authorization", bearer(mentor))).andExpect(status().isForbidden());
        lock(candidate, mentor, "x").andExpect(status().isForbidden());
    }
}
