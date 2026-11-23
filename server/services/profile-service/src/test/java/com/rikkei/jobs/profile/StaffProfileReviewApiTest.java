package com.rikkei.jobs.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:hs06_review;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret", "logging.level.root=WARN"
})
@AutoConfigureMockMvc
class StaffProfileReviewApiTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtils jwt;
    @Autowired ObjectMapper json;
    private final UUID owner = UUID.randomUUID();
    private final UUID reviewer = UUID.randomUUID();
    private final OffsetDateTime submitted = OffsetDateTime.parse("2026-01-01T00:00:00Z");
    private String staff;

    @BeforeEach void setUp() {
        staff = "Bearer " + jwt.generateJwtToken(reviewer, Role.STAFF, null);
        profile(owner, "Candidate", "Demo", "2026", "PENDING", submitted);
    }

    private void profile(UUID id, String name, String classCode, String course, String visibility, OffsetDateTime consent) {
        jdbc.update("""
            insert into profile.candidate_profiles(user_id,full_name,class_code,course_name,visibility,consent_at,
                program,email,phone,address,headline,summary,updated_at)
            values (?,?,?,?,?,?,'Software','private@example.com','0912345678','Private street','Old','About me',CURRENT_TIMESTAMP)
            """, id, name, classCode, course, visibility, consent);
    }

    private MockHttpServletRequestBuilder edit(String field, Object value, String reason) throws Exception {
        return put("/staff/profiles/{id}/self-declared", owner).header("Authorization", staff)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(Map.of("field", field, "value", value, "reason", reason)));
    }

    @Test void queueFiltersBeforePagingAndCountsOnlyRejections() throws Exception {
        UUID first = UUID.fromString("10000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("10000000-0000-0000-0000-000000000002");
        UUID later = UUID.fromString("10000000-0000-0000-0000-000000000000");
        profile(second, "Tran Linh", "C1", "K1", "PENDING", submitted);
        profile(first, "Le Linh", "C1", "K1", "PENDING", submitted);
        profile(later, "Vo Linh", "C1", "K1", "PENDING", submitted.plusDays(1));
        profile(UUID.randomUUID(), "Linh", "C2", "K1", "PENDING", submitted);
        profile(UUID.randomUUID(), "Linh", "C1", "K2", "PENDING", submitted);
        profile(UUID.randomUUID(), "Other", "C1", "K1", "PENDING", submitted);
        profile(UUID.randomUUID(), "Linh", "C1", "K1", "REJECTED", submitted);
        for (String action : List.of("REJECT", "SUBMIT", "REJECT")) {
            jdbc.update("insert into profile.profile_reviews(user_id,action,reason,actor_id,created_at) values (?,?,?,?,CURRENT_TIMESTAMP)",
                first, action, "History", reviewer);
        }
        var query = get("/staff/profile-reviews").header("Authorization", staff)
            .param("classCode", "C1").param("courseName", "K1").param("q", "linh").param("size", "2");
        mvc.perform(query).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3))
            .andExpect(jsonPath("$.items[0].userId").value(first.toString()))
            .andExpect(jsonPath("$.items[0].rejectCount").value(2))
            .andExpect(jsonPath("$.items[0].consentAt").isString())
            .andExpect(jsonPath("$.items[1].userId").value(second.toString()))
            .andExpect(jsonPath("$.items[1].rejectCount").value(0));
        mvc.perform(query.param("page", "2")).andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].userId").value(later.toString()));
        mvc.perform(query.param("status", "REJECTED")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }

    @Test void previewShowsDeclaredContentAndSkillsWithoutPrivateContact() throws Exception {
        mvc.perform(get("/staff/profiles/{id}/preview", owner).header("Authorization", staff))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Candidate"))
            .andExpect(jsonPath("$.headline").value("Old")).andExpect(jsonPath("$.summary").value("About me"))
            .andExpect(jsonPath("$.skills").isArray()).andExpect(jsonPath("$.email").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.phone").doesNotHaveJsonPath()).andExpect(jsonPath("$.address").doesNotHaveJsonPath());
        String candidate = "Bearer " + jwt.generateJwtToken(owner, Role.CANDIDATE, null);
        mvc.perform(post("/profiles/me/withdraw").header("Authorization", candidate)).andExpect(status().isOk());
        mvc.perform(get("/staff/profiles/{id}/preview", owner).header("Authorization", staff)).andExpect(status().isConflict());
        mvc.perform(get("/staff/profiles/{id}/preview", UUID.randomUUID()).header("Authorization", staff))
            .andExpect(status().isNotFound());
        mvc.perform(edit("summary", "Late edit", "Correction")).andExpect(status().isConflict());
        assertThat(count("select count(*) from profile.profile_edits where user_id=?")).isZero();
    }

    @Test void selfSkillReplacementKeepsVerifiedScoreAndEvidenceAndAuditsOnlySuccessfulEdits() throws Exception {
        UUID skill = jdbc.queryForObject("select id from profile.skills where code='JAVA'", UUID.class);
        jdbc.update("insert into profile.skill_scores(user_id,skill_id,verified_score,level,updated_at) values (?,?,82,'GOOD',CURRENT_TIMESTAMP)", owner, skill);
        jdbc.update("""
            insert into profile.skill_evidences(id,user_id,skill_id,source,source_ref,title,score,weight,recorded_at,verified)
            values (?,?,?,'EXAM','exam-ref','Exam',82,1,CURRENT_TIMESTAMP,true)
            """, UUID.randomUUID(), owner, skill);
        mvc.perform(edit("selfSkills", List.of(Map.of("skillId", skill, "selfScore", 35)), "Fix score"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/staff/profiles/{id}/preview", owner).header("Authorization", staff))
            .andExpect(status().isOk()).andExpect(jsonPath("$.skills[0].verifiedScore").value(82))
            .andExpect(jsonPath("$.skills[0].selfScore").value(35));
        mvc.perform(edit("selfSkills", List.of(Map.of("skillId", skill, "selfScore", 101)), "Invalid score"))
            .andExpect(status().isBadRequest());
        mvc.perform(edit("selfSkills", List.of(), "Remove self declaration")).andExpect(status().isNoContent());
        assertThat(count("select count(*) from profile.skill_scores where user_id=? and self_score is null and verified_score=82")).isOne();
        assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='EXAM' and verified and score=82")).isOne();
        assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='SELF'")).isZero();
        assertThat(count("select count(*) from profile.profile_edits where user_id=?")).isEqualTo(2);
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class, owner);
    }

    @Test void headlineChangeRecordsActorReasonAndBothValues() throws Exception {
        mvc.perform(edit("headline", "Updated headline", "  Typo in headline  ")).andExpect(status().isNoContent());
        var audit = jdbc.queryForMap("""
            select staff_id, field, old_value, new_value, reason, created_at
            from profile.profile_edits where user_id=?
            """, owner);
        assertThat(audit.get("staff_id")).isEqualTo(reviewer);
        assertThat(audit.get("field")).isEqualTo("headline");
        assertThat(audit.get("reason")).isEqualTo("Typo in headline");
        assertThat(audit.get("created_at")).isNotNull();
        assertThat(json.readTree((String) audit.get("old_value")).textValue()).isEqualTo("Old");
        assertThat(json.readTree((String) audit.get("new_value")).textValue()).isEqualTo("Updated headline");
        mvc.perform(get("/staff/profiles/{id}/preview", owner).header("Authorization", staff))
            .andExpect(jsonPath("$.headline").value("Updated headline"));
        mvc.perform(edit("summary", "No reason", "   ")).andExpect(status().isBadRequest());
        for (String protectedField : List.of("classCode", "verifiedScore", "visibility", "email")) {
            mvc.perform(edit(protectedField, "Invalid", "Correction")).andExpect(status().isBadRequest());
        }
        assertThat(count("select count(*) from profile.profile_edits where user_id=?")).isOne();
    }

    @Test void reviewReadsAndWritesRequireStaff() throws Exception {
        for (Role role : Role.values()) {
            if (role == Role.STAFF) continue;
            String auth = "Bearer " + jwt.generateJwtToken(owner, role, null);
            mvc.perform(get("/staff/profile-reviews").header("Authorization", auth)).andExpect(status().isForbidden());
            mvc.perform(get("/staff/profiles/{id}/preview", owner).header("Authorization", auth)).andExpect(status().isForbidden());
            mvc.perform(put("/staff/profiles/{id}/self-declared", owner).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"field\":\"summary\",\"value\":\"Changed\",\"reason\":\"Correction\"}"))
                .andExpect(status().isForbidden());
            mvc.perform(post("/staff/profiles/{id}/approve", owner).header("Authorization", auth)).andExpect(status().isForbidden());
            mvc.perform(post("/staff/profiles/{id}/reject", owner).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Incomplete\"}")).andExpect(status().isForbidden());
        }
        assertThat(count("select count(*) from profile.profile_edits where user_id=?")).isZero();
    }

    @Test void approveAndRejectRacingForOnePendingProfileRecordExactlyOneDecision() throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var approve = pool.submit(() -> {
                start.await();
                return mvc.perform(post("/staff/profiles/{id}/approve", owner).header("Authorization", staff))
                    .andReturn().getResponse().getStatus();
            });
            var reject = pool.submit(() -> {
                start.await();
                return mvc.perform(post("/staff/profiles/{id}/reject", owner).header("Authorization", staff)
                    .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Incomplete\"}"))
                    .andReturn().getResponse().getStatus();
            });
            start.countDown();
            assertThat(List.of(approve.get(), reject.get())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(count("select count(*) from profile.profile_reviews where user_id=? and action in ('APPROVE','REJECT')")).isOne();
        mvc.perform(get("/staff/profiles/{id}/preview", owner).header("Authorization", staff)).andExpect(status().isConflict());
        mvc.perform(edit("headline", "Too late", "Review closed")).andExpect(status().isConflict());
        assertThat(count("select count(*) from profile.profile_edits where user_id=?")).isZero();
    }
}
