package com.rikkei.jobs.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.entity.UserStatus;
import com.rikkei.jobs.identity.repository.PasswordResetTokenRepository;
import com.rikkei.jobs.identity.repository.UserRepository;
import com.rikkei.jobs.identity.service.MailService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
            "spring.datasource.url=jdbc:h2:mem:identity-internal-lock;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "app.dev-data.enabled=false",
            "app.lms.base-url=http://localhost:0",
            "app.mail.enabled=false",
            "app.internal.service-key=test-key"
        })
@AutoConfigureMockMvc
class InternalUserLockApiTest {

    private static final String KEY = "test-key";

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
        u.setPasswordHash(encoder.encode("Test@123"));
        return users.saveAndFlush(u);
    }

    private ResultActions lock(UUID id, String reason) throws Exception {
        return mvc.perform(patch("/internal/users/{id}/lock", id)
                .header("X-Internal-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("reason", reason))));
    }

    private ResultActions unlock(UUID id) throws Exception {
        return mvc.perform(patch("/internal/users/{id}/unlock", id).header("X-Internal-Key", KEY));
    }

    private ResultActions lookup(Object ids) throws Exception {
        return mvc.perform(post("/internal/users/lookup")
                .header("X-Internal-Key", KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("userIds", ids))));
    }

    @Test
    void lockSetsAdminLock() throws Exception {
        User u = hr("a@example.com");
        lock(u.getId(), "  Nghỉ việc  ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.lockedReason").value("Nghỉ việc"))
                .andExpect(jsonPath("$.lockedAt").isNotEmpty())
                .andExpect(jsonPath("$.tempLockedUntil").doesNotExist());
        User db = users.findById(u.getId()).orElseThrow();
        assertThat(db.getLockedReason()).isEqualTo("Nghỉ việc");
        assertThat(db.getLockedAt()).isNotNull();
        assertThat(db.getStatus()).isEqualTo(UserStatus.LOCKED);
    }

    @Test
    void lockOverwritesReason() throws Exception {
        User u = hr("b@example.com");
        lock(u.getId(), "Lý do một").andExpect(status().isOk());
        lock(u.getId(), "Lý do hai")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockedReason").value("Lý do hai"));
        assertThat(users.findById(u.getId()).orElseThrow().getLockedReason()).isEqualTo("Lý do hai");
    }

    @Test
    void blankOrLongReasonIs400() throws Exception {
        User u = hr("c@example.com");
        for (String r : List.of("", "   ", "a".repeat(501))) {
            lock(u.getId(), r)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.reason").exists());
        }
        assertThat(users.findById(u.getId()).orElseThrow().getLockedAt()).isNull();
    }

    @Test
    void lockNonHrIs409() throws Exception {
        User u = new User();
        u.setEmail("cand@example.com");
        u.setFullName("Ung Vien");
        u.setRole(Role.CANDIDATE);
        u = users.saveAndFlush(u);
        lock(u.getId(), "x").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFLICT"));
        unlock(u.getId()).andExpect(status().isConflict());
    }

    @Test
    void lockUnknownIs404() throws Exception {
        lock(UUID.randomUUID(), "x").andExpect(status().isNotFound());
        unlock(UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void unlockClearsEverything() throws Exception {
        User u = hr("d@example.com");
        u.setFailedLoginCount(3);
        u.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        users.saveAndFlush(u);
        lock(u.getId(), "Vi phạm").andExpect(status().isOk());

        unlock(u.getId()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));

        User db = users.findById(u.getId()).orElseThrow();
        assertThat(db.getLockedAt()).isNull();
        assertThat(db.getLockedReason()).isNull();
        assertThat(db.getLockedUntil()).isNull();
        assertThat(db.getFailedLoginCount()).isZero();
        assertThat(db.getStatus()).isEqualTo(UserStatus.ACTIVE);
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "d@example.com", "password", "Test@123"))))
                .andExpect(status().isOk());
    }

    @Test
    void unlockWhenNotAdminLockedIsNoop() throws Exception {
        User u = hr("e@example.com");
        Instant until = Instant.now().plus(10, ChronoUnit.MINUTES).truncatedTo(ChronoUnit.MILLIS);
        u.setStatus(UserStatus.LOCKED);
        u.setLockedUntil(until);
        users.saveAndFlush(u);

        unlock(u.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.tempLockedUntil").isNotEmpty());
        User db = users.findById(u.getId()).orElseThrow();
        assertThat(db.getLockedUntil()).isEqualTo(until);
        assertThat(db.getStatus()).isEqualTo(UserStatus.LOCKED);
    }

    @Test
    void lookupReturnsDerivedStatus() throws Exception {
        User admin = hr("l1@example.com");
        lock(admin.getId(), "Khoá").andExpect(status().isOk());
        User temp = hr("l2@example.com");
        temp.setStatus(UserStatus.LOCKED);
        temp.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));
        users.saveAndFlush(temp);
        User expired = hr("l3@example.com");
        expired.setStatus(UserStatus.LOCKED);
        expired.setLockedUntil(Instant.now().minus(10, ChronoUnit.MINUTES));
        users.saveAndFlush(expired);

        String res = lookup(List.of(admin.getId(), temp.getId(), UUID.randomUUID(), expired.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn().getResponse().getContentAsString();
        JsonNode arr = json.readTree(res);
        assertThat(arr.get(0).get("userId").asText()).isEqualTo(admin.getId().toString());
        assertThat(arr.get(0).get("status").asText()).isEqualTo("LOCKED");
        assertThat(arr.get(0).get("email").asText()).isEqualTo("l1@example.com");
        assertThat(arr.get(0).get("fullName").asText()).isEqualTo("HR l1@example.com");
        assertThat(arr.get(1).get("status").asText()).isEqualTo("ACTIVE");
        assertThat(arr.get(1).get("tempLockedUntil").isNull()).isFalse();
        assertThat(arr.get(2).get("userId").asText()).isEqualTo(expired.getId().toString());
        assertThat(arr.get(2).get("status").asText()).isEqualTo("ACTIVE");
        assertThat(arr.get(2).get("tempLockedUntil").isNull()).isTrue();
    }

    @Test
    void lookupDeduplicatesIds() throws Exception {
        User u = hr("dup@example.com");
        lookup(List.of(u.getId(), u.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void lookupValidation() throws Exception {
        lookup(List.of()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        List<UUID> many = java.util.stream.IntStream.range(0, 101).mapToObj(i -> UUID.randomUUID()).toList();
        lookup(many).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingKeyIs401() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(patch("/internal/users/{id}/lock", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"x\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_KEY"));
        mvc.perform(patch("/internal/users/{id}/unlock", id))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_KEY"));
        mvc.perform(post("/internal/users/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\":[\"" + id + "\"]}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_SERVICE_KEY"));
    }
}
