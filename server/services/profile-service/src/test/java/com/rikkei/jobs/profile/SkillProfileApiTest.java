package com.rikkei.jobs.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtProperties;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.profile.dto.VerifiedSkillRecord;
import com.rikkei.jobs.profile.entity.MappingSource;
import com.rikkei.jobs.profile.service.SkillProfileService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=${HS01_TEST_DB_URL:jdbc:h2:mem:hs01_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${HS01_TEST_DB_USER:sa}",
        "spring.datasource.password=${HS01_TEST_DB_PASSWORD:}",
        "app.jwt.secret=test_secret_test_secret_test_secret_test_secret",
        "spring.profiles.active=test",
        "spring.jpa.show-sql=false",
        "logging.level.root=WARN"
})
@AutoConfigureMockMvc
class SkillProfileApiTest {
    private static final String SECRET = "test_secret_test_secret_test_secret_test_secret";
    private static final Instant RECORDED_AT = Instant.parse("2026-10-20T08:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired SkillProfileService core;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @Test
    void missingTokenCannotReadSkills() throws Exception {
        mvc.perform(get("/profiles/me/skills"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidTokenCannotReadSkills() throws Exception {
        mvc.perform(get("/profiles/me/skills").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void expiredTokenCannotReadSkills() throws Exception {
        JwtProperties expired = new JwtProperties();
        expired.setSecret(SECRET);
        expired.setExpirationMs(-1000);
        String token = new JwtUtils(expired).generateJwtToken(UUID.randomUUID(), Role.CANDIDATE, null);
        read("/profiles/me/skills", token)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void tokenRequiringPasswordChangeCannotReadSkills() throws Exception {
        read("/profiles/me/skills", jwt.generateJwtToken(UUID.randomUUID(), Role.HR, UUID.randomUUID(), true))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MUST_CHANGE_PASSWORD"));
    }

    @Test
    void candidateReadsOwnSeededSkillsUsingJwtIdentity() throws Exception {
        UUID owner = seededCandidate("85");
        String token = token(owner, Role.CANDIDATE);
        JsonNode own = body(read("/profiles/me/skills", token).andExpect(status().isOk()));
        assertThat(javaSkill(own).path("verifiedScore").decimalValue()).isEqualByComparingTo("85");
        assertThat(javaSkill(own).path("level").asText()).isEqualTo("EXCELLENT");
        assertThat(javaSkill(own).path("evidences").get(0).path("verified").asBoolean()).isTrue();
        assertThat(body(read("/profiles/" + owner + "/skills", token).andExpect(status().isOk())))
                .isEqualTo(own);
    }

    @Test
    void meDoesNotAcceptAnotherUserIdFromQuery() throws Exception {
        UUID owner = seededCandidate("85");
        UUID other = seededCandidate("45");
        JsonNode own = body(read("/profiles/me/skills?userId=" + other, token(owner, Role.CANDIDATE))
                .andExpect(status().isOk()));
        assertThat(javaSkill(own).path("verifiedScore").decimalValue()).isEqualByComparingTo("85");
    }

    @Test
    void hrCannotUseCandidateMeRoute() throws Exception {
        read("/profiles/me/skills", token(UUID.randomUUID(), Role.HR))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    void anotherUserCannotDistinguishPrivateFromMissingProfile(Role viewerRole) throws Exception {
        UUID owner = seededCandidate("85");
        String viewer = token(UUID.randomUUID(), viewerRole);
        JsonNode privateError = body(read("/profiles/" + owner + "/skills", viewer)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND")));
        JsonNode missingError = body(read("/profiles/" + UUID.randomUUID() + "/skills", viewer)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND")));
        assertThat(privateError).isEqualTo(missingError);
    }

    @Test
    void consentWithoutApprovalDoesNotPublishProfile() throws Exception {
        UUID owner = seededCandidate("85");
        publication(owner, true, false);
        read("/profiles/" + owner + "/skills", token(UUID.randomUUID(), Role.HR))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void approvalWithoutConsentDoesNotPublishProfile() throws Exception {
        UUID owner = seededCandidate("85");
        publication(owner, false, true);
        read("/profiles/" + owner + "/skills", token(UUID.randomUUID(), Role.HR))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void bothPublicationFlagsAllowReadAndRevocationHidesItImmediately() throws Exception {
        UUID owner = seededCandidate("85");
        String viewer = token(UUID.randomUUID(), Role.HR);
        publication(owner, true, true);
        JsonNode published = body(read("/profiles/" + owner + "/skills", viewer).andExpect(status().isOk()));
        assertThat(javaSkill(published).path("verifiedScore").decimalValue()).isEqualByComparingTo("85");
        publication(owner, false, true);
        read("/profiles/" + owner + "/skills", viewer)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void malformedUserIdReturnsBadRequest() throws Exception {
        read("/profiles/not-a-uuid/skills", token(UUID.randomUUID(), Role.HR))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void recomputeUsesStoredSourceForJwtOwnerWithoutMutatingOtherUser() throws Exception {
        UUID owner = seededCandidate("85");
        UUID other = seededCandidate("45");
        var otherBefore = core.getSkills(other);
        jdbc.update("INSERT INTO profile.external_records "
                        + "(id,user_id,source,source_code,source_ref,title,score,score_max,recorded_at,verified) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID(), owner, "LMS", "JAVA01", "api-recomputed-java", "Official Java result", new BigDecimal("9"),
                new BigDecimal("10"), RECORDED_AT.atOffset(java.time.ZoneOffset.UTC), true);
        JsonNode recomputed = body(mvc.perform(post("/profiles/me/skills/recompute").queryParam("userId", other.toString())
                        .header("Authorization", "Bearer " + token(owner, Role.CANDIDATE)))
                .andExpect(status().isOk()));
        assertThat(javaSkill(recomputed).path("evidences").findValuesAsText("title"))
                .contains("Official Java result");
        assertThat(core.getSkills(other)).isEqualTo(otherBefore);
    }

    @Test
    void hrCannotRecomputeCandidateSkills() throws Exception {
        mvc.perform(post("/profiles/me/skills/recompute")
                        .header("Authorization", "Bearer " + token(UUID.randomUUID(), Role.HR)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private UUID seededCandidate(String score) {
        UUID user = UUID.randomUUID();
        core.importVerifiedRecords(user, List.of(new VerifiedSkillRecord(MappingSource.LMS, "JAVA01", "java-1",
                "Official Java seed", new BigDecimal(score), RECORDED_AT)));
        return user;
    }

    private void publication(UUID user, boolean consented, boolean approved) {
        assertThat(jdbc.update("UPDATE profile.skill_profile_states SET consented=?, approved=? WHERE user_id=?",
                consented, approved, user)).isEqualTo(1);
    }

    private String token(UUID user, Role role) {
        return jwt.generateJwtToken(user, role, null);
    }

    private ResultActions read(String path, String token) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    private JsonNode body(ResultActions response) throws Exception {
        return json.readTree(response.andReturn().getResponse().getContentAsByteArray());
    }

    private JsonNode javaSkill(JsonNode skills) {
        for (JsonNode skill : skills) {
            if (skill.path("skill").path("code").asText().equals("JAVA")) {
                return skill;
            }
        }
        throw new AssertionError("Expected JAVA skill in " + skills);
    }
}
