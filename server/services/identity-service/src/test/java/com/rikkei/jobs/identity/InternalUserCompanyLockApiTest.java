package com.rikkei.jobs.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.repository.PasswordResetTokenRepository;
import com.rikkei.jobs.identity.repository.UserRepository;
import com.rikkei.jobs.identity.service.MailService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:identity-company-lock;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "app.dev-data.enabled=false",
            "app.lms.base-url=http://localhost:0",
            "app.mail.enabled=false",
            "app.internal.service-key=test-key"
        })
@AutoConfigureMockMvc
class InternalUserCompanyLockApiTest {

    private static final String KEY = "test-key";
    private static final String PASSWORD = "Test@123";
    private static final String COMPANY_MSG = "Công ty của bạn đang bị khoá, liên hệ cán bộ tuyển dụng";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired PasswordEncoder encoder;

    @MockBean MailService mail;

    @BeforeEach
    void clean() {
        resetTokens.deleteAll();
        users.deleteAll();
    }

    private User hr(String email) {
        User u = new User();
        u.setEmail(email);
        u.setFullName("HR " + email);
        u.setRole(Role.HR);
        u.setCompanyId(UUID.randomUUID());
        u.setPasswordHash(encoder.encode(PASSWORD));
        return users.saveAndFlush(u);
    }

    private ResultActions companyLock(Object ids, boolean locked) throws Exception {
        return mvc.perform(post("/internal/users/company-lock")
                .header("X-Internal-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("userIds", ids, "locked", locked))));
    }

    private ResultActions login(String email) throws Exception {
        return mvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("email", email, "password", PASSWORD))));
    }

    private ResultActions lookup(UUID id) throws Exception {
        return mvc.perform(post("/internal/users/lookup")
                .header("X-Internal-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("userIds", List.of(id)))));
    }

    @Test
    void requiresInternalKey() throws Exception {
        String body = "{\"userIds\":[\"" + UUID.randomUUID() + "\"],\"locked\":true}";
        mvc.perform(post("/internal/users/company-lock").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/internal/users/company-lock")
                        .header("X-Internal-Key", "wrong")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lockBlocksLoginTokenAndPasswordReset() throws Exception {
        User u = hr("hr1@example.com");
        String res = login("hr1@example.com").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(res).get("accessToken").asText();

        companyLock(List.of(u.getId()), true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("LOCKED"))
                .andExpect(jsonPath("$[0].companyLocked").value(true));

        login("hr1@example.com")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"))
                .andExpect(jsonPath("$.message").value(COMPANY_MSG));
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        mvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"hr1@example.com\"}"))
                .andExpect(status().isOk());
        verify(mail, never()).sendPasswordReset(anyString(), anyString(), anyString(), anyLong());
        assertThat(resetTokens.findAll()).isEmpty();
    }

    /** Sai mật khẩu trả lỗi như mọi tài khoản khác, để không ai dò được công ty nào đang bị khoá. */
    @Test
    void companyLockedWrongPasswordIsInvalidCredentials() throws Exception {
        User u = hr("hr6@example.com");
        companyLock(List.of(u.getId()), true).andExpect(status().isOk());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "hr6@example.com", "password", "wrong-pass-1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void unlockRestoresLogin() throws Exception {
        User u = hr("hr2@example.com");
        companyLock(List.of(u.getId()), true).andExpect(status().isOk());
        companyLock(List.of(u.getId()), false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].companyLocked").value(false));
        login("hr2@example.com").andExpect(status().isOk());
    }

    @Test
    void individualLockSurvivesCompanyUnlock() throws Exception {
        User u = hr("hr3@example.com");
        mvc.perform(patch("/internal/users/{id}/lock", u.getId())
                        .header("X-Internal-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Vi phạm\"}"))
                .andExpect(status().isOk());
        companyLock(List.of(u.getId()), true).andExpect(status().isOk());
        companyLock(List.of(u.getId()), false).andExpect(status().isOk());

        login("hr3@example.com")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"))
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khoá"));
        lookup(u.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("LOCKED"))
                .andExpect(jsonPath("$[0].companyLocked").value(false));
    }

    @Test
    void individualUnlockKeepsCompanyLock() throws Exception {
        User u = hr("hr4@example.com");
        companyLock(List.of(u.getId()), true).andExpect(status().isOk());
        mvc.perform(patch("/internal/users/{id}/unlock", u.getId()).header("X-Internal-Key", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.companyLocked").value(true));
        login("hr4@example.com").andExpect(status().isForbidden()).andExpect(jsonPath("$.message").value(COMPANY_MSG));
    }

    @Test
    void nonHrIdsIgnoredAndRelockKeepsTimestamp() throws Exception {
        User u = hr("hr5@example.com");
        User cand = new User();
        cand.setEmail("cand@example.com");
        cand.setFullName("Ung Vien");
        cand.setRole(Role.CANDIDATE);
        cand = users.saveAndFlush(cand);

        companyLock(List.of(u.getId(), cand.getId(), UUID.randomUUID()), true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(u.getId().toString()));
        assertThat(users.findById(cand.getId()).orElseThrow().getCompanyLockedAt()).isNull();

        var first = users.findById(u.getId()).orElseThrow().getCompanyLockedAt();
        assertThat(first).isNotNull();
        Thread.sleep(20);
        companyLock(List.of(u.getId()), true).andExpect(status().isOk());
        assertThat(users.findById(u.getId()).orElseThrow().getCompanyLockedAt()).isEqualTo(first);
    }

    @Test
    void invalidBody() throws Exception {
        companyLock(List.of(), true).andExpect(status().isBadRequest());
        List<UUID> many = java.util.stream.IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).toList();
        companyLock(many, true).andExpect(status().isBadRequest());
    }
}
