package com.rikkei.jobs.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.identity.client.LmsClient;
import com.rikkei.jobs.identity.dto.response.LmsTicketInfo;
import com.rikkei.jobs.identity.entity.PasswordResetToken;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.entity.User;
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
            "spring.datasource.url=jdbc:h2:mem:identity;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "app.dev-data.enabled=false",
            "app.lms.base-url=http://localhost:0",
            "app.mail.enabled=false"
        })
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "Temp@1234";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired PasswordEncoder encoder;

    @Autowired com.rikkei.jobs.common.config.security.jwt.JwtUtils jwtUtils;

    @MockBean LmsClient lms;
    @MockBean MailService mail;

    @BeforeEach
    void clean() {
        resetTokens.deleteAll();
        users.deleteAll();
    }

    // ---------- helpers ----------

    private ResultActions postJson(String url, Object body, String token) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(req);
    }

    private User hrUser(String email, boolean mustChange) {
        User u = new User();
        u.setEmail(email);
        u.setFullName("HR " + email);
        u.setRole(Role.HR);
        u.setCompanyId(UUID.randomUUID());
        u.setPasswordHash(encoder.encode(PASSWORD));
        u.setMustChangePassword(mustChange);
        return users.save(u);
    }

    private void stubTicket(String ticket, Role role, Instant issuedAt) {
        when(lms.verify(ticket))
                .thenReturn(Optional.of(new LmsTicketInfo("lms-1", "candidate1@rikkeijobs.test", "Candidate One", role, issuedAt)));
    }

    private String loginToken(String email) throws Exception {
        String body = postJson("/auth/login", Map.of("email", email, "password", PASSWORD), null)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return json.readTree(body).get("accessToken").asText();
    }

    // ---------- SSO ----------

    @Test
    void ssoCreatesUserWithMockRoleAndMeReturnsIt() throws Exception {
        stubTicket("t-1", Role.CANDIDATE, Instant.now());

        String body = postJson("/auth/sso", Map.of("ticket", "t-1"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("CANDIDATE"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = json.readTree(body).get("accessToken").asText();

        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CANDIDATE"))
                .andExpect(jsonPath("$.email").value("candidate1@rikkeijobs.test"));
        assertThat(users.findByLmsUserId("lms-1")).isPresent();
    }

    @Test
    void ssoSecondLoginReusesUser() throws Exception {
        stubTicket("t-a", Role.CANDIDATE, Instant.now());
        stubTicket("t-b", Role.CANDIDATE, Instant.now());

        postJson("/auth/sso", Map.of("ticket", "t-a"), null).andExpect(status().isOk());
        postJson("/auth/sso", Map.of("ticket", "t-b"), null).andExpect(status().isOk());

        assertThat(users.count()).isEqualTo(1);
    }

    @Test
    void reusedTicketIsRejected() throws Exception {
        stubTicket("t-2", Role.MENTOR, Instant.now());

        postJson("/auth/sso", Map.of("ticket", "t-2"), null).andExpect(status().isOk());
        postJson("/auth/sso", Map.of("ticket", "t-2"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TICKET_ALREADY_USED"));
    }

    @Test
    void ticketOlderThan60SecondsIsRejectedAndNotConsumed() throws Exception {
        stubTicket("t-3", Role.CANDIDATE, Instant.now().minusSeconds(61));

        postJson("/auth/sso", Map.of("ticket", "t-3"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TICKET_EXPIRED"));
        assertThat(users.count()).isZero();
    }

    @Test
    void unknownTicketIsRejected() throws Exception {
        when(lms.verify("nope")).thenReturn(Optional.empty());

        postJson("/auth/sso", Map.of("ticket", "nope"), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TICKET"));
    }

    @Test
    void ssoCannotSignInAsHr() throws Exception {
        stubTicket("t-4", Role.HR, Instant.now());

        postJson("/auth/sso", Map.of("ticket", "t-4"), null).andExpect(status().isUnauthorized());
    }

    @Test
    void ssoCannotTakeOverHrAccountByEmail() throws Exception {
        hrUser("candidate1@rikkeijobs.test", false);
        stubTicket("t-5", Role.CANDIDATE, Instant.now());

        postJson("/auth/sso", Map.of("ticket", "t-5"), null).andExpect(status().isUnauthorized());
    }

    // ---------- login / must change password ----------

    @Test
    void firstLoginMustChangePasswordAndOtherApisAreForbidden() throws Exception {
        hrUser("hr1@acme.test", true);

        String body = postJson("/auth/login", Map.of("email", "hr1@acme.test", "password", PASSWORD), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = json.readTree(body).get("accessToken").asText();

        mvc.perform(get("/api/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MUST_CHANGE_PASSWORD"));
        mvc.perform(get("/auth/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        String changed = postJson("/auth/change-password", Map.of("oldPassword", PASSWORD, "newPassword", "Newpass99"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn().getResponse().getContentAsString();
        // Token cấp lúc đăng nhập mang claim mustChangePassword (các service khác chặn bằng claim này), token mới thì không.
        assertThat(jwtUtils.getPrincipalFromJwtToken(token).isMustChangePassword()).isTrue();
        assertThat(jwtUtils.getPrincipalFromJwtToken(json.readTree(changed).get("accessToken").asText())
                .isMustChangePassword()).isFalse();

        // Same token works for other APIs now (404 = got past the filter, route does not exist).
        mvc.perform(get("/api/anything").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        assertThat(users.findByEmail("hr1@acme.test").orElseThrow().getMustChangePassword()).isFalse();
    }

    @Test
    void jwtCarriesUserIdRoleAndCompanyId() throws Exception {
        User hr = hrUser("hr2@acme.test", false);
        String token = loginToken("hr2@acme.test");

        JsonNode payload = json.readTree(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertThat(payload.get("sub").asText()).isEqualTo(hr.getId().toString());
        assertThat(payload.get("role").asText()).isEqualTo("HR");
        assertThat(payload.get("companyId").asText()).isEqualTo(hr.getCompanyId().toString());
        assertThat(payload.get("exp").asLong() - payload.get("iat").asLong()).isEqualTo(8 * 3600);
    }

    @Test
    void wrongPasswordFiveTimesLocksAccountEvenForCorrectPassword() throws Exception {
        hrUser("hr1@acme.test", false);

        for (int i = 0; i < 5; i++) {
            postJson("/auth/login", Map.of("email", "hr1@acme.test", "password", "wrong-pass-1"), null)
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }
        postJson("/auth/login", Map.of("email", "hr1@acme.test", "password", PASSWORD), null)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.errors.minutesRemaining").value(15));
    }

    @Test
    void successfulLoginResetsFailedCounter() throws Exception {
        hrUser("hr1@acme.test", false);

        for (int i = 0; i < 4; i++) {
            postJson("/auth/login", Map.of("email", "hr1@acme.test", "password", "wrong-pass-1"), null)
                    .andExpect(status().isUnauthorized());
        }
        postJson("/auth/login", Map.of("email", "hr1@acme.test", "password", PASSWORD), null)
                .andExpect(status().isOk());
        assertThat(users.findByEmail("hr1@acme.test").orElseThrow().getFailedLoginCount()).isZero();

        // Four more wrong attempts still do not lock, because counting restarted from 0.
        for (int i = 0; i < 4; i++) {
            postJson("/auth/login", Map.of("email", "hr1@acme.test", "password", "wrong-pass-1"), null)
                    .andExpect(status().isUnauthorized());
        }
        postJson("/auth/login", Map.of("email", "hr1@acme.test", "password", PASSWORD), null)
                .andExpect(status().isOk());
    }

    @Test
    void lockExpiresAfterLockPeriod() throws Exception {
        User hr = hrUser("hr1@acme.test", false);
        hr.setFailedLoginCount(5);
        hr.setStatus(com.rikkei.jobs.identity.entity.UserStatus.LOCKED);
        hr.setLockedUntil(Instant.now().minusSeconds(1));
        users.save(hr);

        postJson("/auth/login", Map.of("email", "hr1@acme.test", "password", PASSWORD), null)
                .andExpect(status().isOk());
    }

    @Test
    void unknownEmailGivesSameErrorAsWrongPassword() throws Exception {
        postJson("/auth/login", Map.of("email", "ghost@acme.test", "password", PASSWORD), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void protectedEndpointWithoutTokenIs401() throws Exception {
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    // ---------- change password / policy / hashing ----------

    @Test
    void passwordIsStoredHashed() {
        User hr = hrUser("hr1@acme.test", false);

        String stored = users.findById(hr.getId()).orElseThrow().getPasswordHash();
        assertThat(stored).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(encoder.matches(PASSWORD, stored)).isTrue();
    }

    @Test
    void weakNewPasswordIsRejected() throws Exception {
        hrUser("hr1@acme.test", false);
        String token = loginToken("hr1@acme.test");

        for (String weak : new String[] {"short1", "onlyletters", "12345678"}) {
            postJson("/auth/change-password", Map.of("oldPassword", PASSWORD, "newPassword", weak), token)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.newPassword").exists());
        }
    }

    @Test
    void changePasswordRejectsWrongOldPassword() throws Exception {
        hrUser("hr1@acme.test", false);
        String token = loginToken("hr1@acme.test");

        postJson("/auth/change-password", Map.of("oldPassword", "not-the-old-1", "newPassword", "Newpass99"), token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OLD_PASSWORD"));
    }

    // ---------- forgot / reset ----------

    @Test
    void forgotPasswordAlwaysReturns200() throws Exception {
        postJson("/auth/forgot-password", Map.of("email", "ghost@acme.test"), null).andExpect(status().isOk());
        verify(mail, never()).sendPasswordReset(anyString(), anyString(), anyString(), anyLong());
    }

    @Test
    void resetLinkWorksOnceAndThenReportsExpired() throws Exception {
        hrUser("hr1@acme.test", true);

        postJson("/auth/forgot-password", Map.of("email", "HR1@acme.test"), null).andExpect(status().isOk());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mail).sendPasswordReset(eq("hr1@acme.test"), anyString(), link.capture(), eq(30L));
        assertThat(link.getValue()).contains("/reset-password/");
        String token = link.getValue().substring(link.getValue().lastIndexOf('/') + 1);

        // The DB holds only a hash of the token.
        assertThat(resetTokens.findAll()).hasSize(1).allSatisfy(t -> assertThat(t.getTokenHash()).isNotEqualTo(token));

        postJson("/auth/reset-password", Map.of("token", token, "newPassword", "Brandnew123"), null)
                .andExpect(status().isOk());
        postJson("/auth/login", Map.of("email", "hr1@acme.test", "password", "Brandnew123"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(false));

        postJson("/auth/reset-password", Map.of("token", token, "newPassword", "Another123"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
    }

    @Test
    void expiredResetTokenIsRejected() throws Exception {
        hrUser("hr1@acme.test", false);
        postJson("/auth/forgot-password", Map.of("email", "hr1@acme.test"), null).andExpect(status().isOk());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mail).sendPasswordReset(anyString(), anyString(), link.capture(), anyLong());
        String token = link.getValue().substring(link.getValue().lastIndexOf('/') + 1);

        PasswordResetToken row = resetTokens.findAll().get(0);
        row.setExpiresAt(Instant.now().minusSeconds(1));
        resetTokens.save(row);

        postJson("/auth/reset-password", Map.of("token", token, "newPassword", "Brandnew123"), null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RESET_TOKEN_INVALID"));
    }

    @Test
    void resetUnlocksLockedAccount() throws Exception {
        hrUser("hr1@acme.test", false);
        for (int i = 0; i < 5; i++) {
            postJson("/auth/login", Map.of("email", "hr1@acme.test", "password", "wrong-pass-1"), null);
        }
        postJson("/auth/forgot-password", Map.of("email", "hr1@acme.test"), null).andExpect(status().isOk());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(mail).sendPasswordReset(anyString(), anyString(), link.capture(), anyLong());
        String token = link.getValue().substring(link.getValue().lastIndexOf('/') + 1);

        postJson("/auth/reset-password", Map.of("token", token, "newPassword", "Brandnew123"), null)
                .andExpect(status().isOk());
        postJson("/auth/login", Map.of("email", "hr1@acme.test", "password", "Brandnew123"), null)
                .andExpect(status().isOk());
    }
}
