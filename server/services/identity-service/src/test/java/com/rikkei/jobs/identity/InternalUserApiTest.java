package com.rikkei.jobs.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.repository.PasswordResetTokenRepository;
import com.rikkei.jobs.identity.repository.UserRepository;
import com.rikkei.jobs.identity.service.MailService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:identity-internal;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "app.dev-data.enabled=false",
            "app.lms.base-url=http://localhost:0",
            "app.mail.enabled=false",
            "app.internal.service-key=test-key"
        })
@AutoConfigureMockMvc
class InternalUserApiTest {

    private static final String KEY = "test-key";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired PasswordResetTokenRepository resetTokens;
    @Autowired JwtUtils jwtUtils;

    @MockBean MailService mail;

    @BeforeEach
    void clean() {
        resetTokens.deleteAll();
        users.deleteAll();
    }

    private ResultActions postUser(Object body, String key) throws Exception {
        var req = post("/internal/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body));
        if (key != null) {
            req.header("X-Internal-Key", key);
        }
        return mvc.perform(req);
    }

    private Map<String, Object> body(String email, String fullName, String role, UUID companyId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("email", email);
        m.put("fullName", fullName);
        m.put("role", role);
        if (companyId != null) {
            m.put("companyId", companyId.toString());
        }
        return m;
    }

    @Test
    void createsHrWithMustChangePasswordAndEmailsTempPassword() throws Exception {
        UUID companyId = UUID.randomUUID();
        postUser(body("hr3@rikkeijobs.test", "HR Ba", "HR", companyId), KEY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("HR"))
                .andExpect(jsonPath("$.companyId").value(companyId.toString()));

        User saved = users.findByEmail("hr3@rikkeijobs.test").orElseThrow();
        assertThat(saved.getMustChangePassword()).isTrue();
        assertThat(saved.getPasswordHash()).isNotNull();

        ArgumentCaptor<String> pw = ArgumentCaptor.forClass(String.class);
        verify(mail).sendHrAccountCreated(
                eq("hr3@rikkeijobs.test"), eq("HR Ba"), pw.capture(), eq("http://localhost:3000/login"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                Map.of("email", "hr3@rikkeijobs.test", "password", pw.getValue()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mustChangePassword").value(true));
    }

    @Test
    void normalizesEmail() throws Exception {
        postUser(body(" HR4@Example.COM ", "HR Bon", "HR", UUID.randomUUID()), KEY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("hr4@example.com"));
    }

    @Test
    void duplicateEmailIs409WithExistingUserDetails() throws Exception {
        UUID companyId = UUID.randomUUID();
        String res = postUser(body("hr5@example.com", "HR Nam", "HR", companyId), KEY)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = json.readTree(res);

        postUser(body("HR5@example.com", "HR Nam", "HR", UUID.randomUUID()), KEY)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"))
                .andExpect(jsonPath("$.errors.userId").value(created.get("userId").asText()))
                .andExpect(jsonPath("$.errors.role").value("HR"))
                .andExpect(jsonPath("$.errors.companyId").value(companyId.toString()));
    }

    @Test
    void mailFailureRollsBackUser() throws Exception {
        doThrow(new MailSendException("x")).when(mail).sendHrAccountCreated(any(), any(), any(), any());
        postUser(body("hr6@example.com", "HR Sau", "HR", UUID.randomUUID()), KEY)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("MAIL_UNAVAILABLE"));
        assertThat(users.existsByEmail("hr6@example.com")).isFalse();
    }

    @Test
    void nonHrRoleIs400() throws Exception {
        postUser(body("c1@example.com", "Ung Vien", "CANDIDATE", UUID.randomUUID()), KEY)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingCompanyIdIs400() throws Exception {
        postUser(body("hr7@example.com", "HR Bay", "HR", null), KEY)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingOrWrongKeyIs401() throws Exception {
        Map<String, Object> b = body("hr8@example.com", "HR Tam", "HR", UUID.randomUUID());
        postUser(b, null).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_KEY"));
        postUser(b, "wrong").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_KEY"));
        mvc.perform(delete("/internal/users/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_KEY"));
        mvc.perform(delete("/internal/users/" + UUID.randomUUID()).header("X-Internal-Key", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_KEY"));
        assertThat(users.existsByEmail("hr8@example.com")).isFalse();
    }

    @Test
    void deleteRemovesHr() throws Exception {
        String res = postUser(body("hr9@example.com", "HR Chin", "HR", UUID.randomUUID()), KEY)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(json.readTree(res).get("userId").asText());

        mvc.perform(delete("/internal/users/" + id).header("X-Internal-Key", KEY))
                .andExpect(status().isNoContent());
        assertThat(users.findById(id)).isEmpty();
        mvc.perform(delete("/internal/users/" + id).header("X-Internal-Key", KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void deleteRefusesNonHr() throws Exception {
        User u = new User();
        u.setEmail("cand@example.com");
        u.setFullName("Ung Vien");
        u.setRole(Role.CANDIDATE);
        u = users.save(u);

        mvc.perform(delete("/internal/users/" + u.getId()).header("X-Internal-Key", KEY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
        assertThat(users.findById(u.getId())).isPresent();
    }

    @Test
    void internalPathIsNotReachableWithJwtOnly() throws Exception {
        String token = jwtUtils.generateJwtToken(UUID.randomUUID(), Role.STAFF, null);
        mvc.perform(post("/internal/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                body("hr10@example.com", "HR Muoi", "HR", UUID.randomUUID()))))
                .andExpect(status().isUnauthorized());
    }
}
