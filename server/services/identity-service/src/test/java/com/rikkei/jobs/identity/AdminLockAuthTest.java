package com.rikkei.jobs.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.client.LmsClient;
import com.rikkei.jobs.identity.dto.response.LmsTicketInfo;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.entity.UserStatus;
import com.rikkei.jobs.identity.repository.PasswordResetTokenRepository;
import com.rikkei.jobs.identity.repository.UserRepository;
import com.rikkei.jobs.identity.service.MailService;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
            "spring.datasource.url=jdbc:h2:mem:identity-admin-lock;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "app.dev-data.enabled=false",
            "app.lms.base-url=http://localhost:0",
            "app.mail.enabled=false"
        })
@AutoConfigureMockMvc
class AdminLockAuthTest {

    private static final String PASSWORD = "Test@123";
    private static final String EMAIL = "hr@lock.test";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired PasswordEncoder encoder;

    @MockBean LmsClient lms;
    @MockBean MailService mail;

    @BeforeEach
    void clean() {
        resetTokens.deleteAll();
        users.deleteAll();
    }

    private ResultActions postJson(String url, Object body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private User hr(boolean mustChange) {
        User u = new User();
        u.setEmail(EMAIL);
        u.setFullName("HR Lock");
        u.setRole(Role.HR);
        u.setCompanyId(UUID.randomUUID());
        u.setPasswordHash(encoder.encode(PASSWORD));
        u.setMustChangePassword(mustChange);
        return users.save(u);
    }

    private void adminLock(User u) {
        u.setStatus(UserStatus.LOCKED);
        u.setLockedAt(Instant.now());
        u.setLockedReason("Nghỉ việc");
        users.save(u);
    }

    private ResultActions login(String password) throws Exception {
        return postJson("/auth/login", Map.of("email", EMAIL, "password", password));
    }

    @Test
    void adminLockedLoginIs403AccountDisabled() throws Exception {
        User u = hr(false);
        adminLock(u);
        login(PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"))
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khoá"));
        User after = users.findById(u.getId()).orElseThrow();
        assertThat(after.getFailedLoginCount()).isZero();
        assertThat(after.getLockedAt()).isNotNull();
    }

    @Test
    void adminLockedWrongPasswordStillAccountDisabled() throws Exception {
        User u = hr(false);
        adminLock(u);
        login("wrong-pass-1").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        assertThat(users.findById(u.getId()).orElseThrow().getFailedLoginCount()).isZero();
    }

    @Test
    void oldTokenRejectedOnNextRequest() throws Exception {
        User u = hr(false);
        String body = login(PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("accessToken").asText();
        adminLock(users.findById(u.getId()).orElseThrow());

        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void adminLockCheckedBeforeMustChangePassword() throws Exception {
        User u = hr(true);
        String body = login(PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = json.readTree(body).get("accessToken").asText();
        adminLock(users.findById(u.getId()).orElseThrow());

        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void tempLockUnchanged() throws Exception {
        User u = hr(false);
        for (int i = 0; i < 5; i++) {
            login("wrong-pass-1").andExpect(status().isUnauthorized());
        }
        login(PASSWORD).andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

        User locked = users.findById(u.getId()).orElseThrow();
        assertThat(locked.getLockedAt()).isNull();
        locked.setLockedUntil(Instant.now().minusSeconds(5));
        users.save(locked);
        login(PASSWORD).andExpect(status().isOk());
    }

    @Test
    void forgotPasswordSkipsEmailWhenAdminLocked() throws Exception {
        adminLock(hr(false));
        postJson("/auth/forgot-password", Map.of("email", EMAIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Nếu email tồn tại trong hệ thống, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu"));
        verify(mail, never()).sendPasswordReset(anyString(), anyString(), anyString(), anyLong());
        assertThat(resetTokens.findAll()).isEmpty();
    }

    @Test
    void resetPasswordKeepsAdminLock() throws Exception {
        User u = hr(false);
        postJson("/auth/forgot-password", Map.of("email", EMAIL)).andExpect(status().isOk());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mail).sendPasswordReset(anyString(), anyString(), link.capture(), anyLong());
        String token = link.getValue().substring(link.getValue().lastIndexOf('/') + 1);

        adminLock(users.findById(u.getId()).orElseThrow());
        String oldHash = users.findById(u.getId()).orElseThrow().getPasswordHash();

        postJson("/auth/reset-password", Map.of("token", token, "newPassword", "Brandnew123"))
                .andExpect(status().isOk());

        User after = users.findById(u.getId()).orElseThrow();
        assertThat(after.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(encoder.matches("Brandnew123", after.getPasswordHash())).isTrue();
        assertThat(after.getStatus()).isEqualTo(UserStatus.LOCKED);
        assertThat(after.getLockedAt()).isNotNull();
        assertThat(after.getLockedReason()).isEqualTo("Nghỉ việc");

        postJson("/auth/login", Map.of("email", EMAIL, "password", "Brandnew123"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void ssoAdminLockedIs403() throws Exception {
        User u = new User();
        u.setEmail("sv@lock.test");
        u.setFullName("Sinh vien");
        u.setRole(Role.CANDIDATE);
        u.setLmsUserId("lms-lock-1");
        adminLock(users.save(u));

        when(lms.verify("t-lock"))
                .thenReturn(Optional.of(new LmsTicketInfo(
                        "lms-lock-1", "sv@lock.test", "Sinh vien", Role.CANDIDATE, Instant.now())));
        postJson("/auth/sso", Map.of("ticket", "t-lock"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }
}
