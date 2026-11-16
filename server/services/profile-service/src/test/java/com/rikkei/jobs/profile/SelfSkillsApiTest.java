package com.rikkei.jobs.profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.math.BigDecimal;
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
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=${HS02_PHASE3_TEST_DB_URL:jdbc:h2:mem:hs02_self;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${HS02_PHASE3_TEST_DB_USER:sa}", "spring.datasource.password=${HS02_PHASE3_TEST_DB_PASSWORD:}",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret", "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
class SelfSkillsApiTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtils jwt;
    @Autowired ObjectMapper json;
    UUID owner, java, english;
    String bearer;
    @BeforeEach void setup() {
        owner = UUID.randomUUID(); bearer = "Bearer " + jwt.generateJwtToken(owner, Role.CANDIDATE, null);
        java = jdbc.queryForObject("select id from profile.skills where code='JAVA'", UUID.class);
        english = jdbc.queryForObject("select id from profile.skills where code='ENGLISH'", UUID.class);
    }
    @Test void catalogueRequiresAuthAndReturnsDatabaseSkillsInStableOrder() throws Exception {
        mvc.perform(get("/skills")).andExpect(status().isUnauthorized());
        JsonNode list = json.readTree(mvc.perform(get("/skills").header("Authorization", bearer))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(list.size()).isEqualTo(jdbc.queryForObject("select count(*) from profile.skills", Integer.class));
        boolean found = false;
        String previous = "";
        for (JsonNode skill : list) {
            if (skill.path("id").asText().equals(java.toString())) found = true;
            String order = skill.path("group").asText() + "|" + skill.path("code").asText();
            assertThat(order.compareTo(previous)).isGreaterThanOrEqualTo(0); previous = order;
            assertThat(skill.path("name").asText()).isNotBlank();
        }
        assertThat(found).isTrue();
    }
    @Test void replacesSelfScoresAndEvidenceWithoutChangingVerifiedDataOrAnotherUser() throws Exception {
        UUID other = UUID.randomUUID();
        seedVerified(owner); seedVerified(other);
        save("[" + entry(java, "70") + "," + entry(english, "55.123456") + "]", "?userId=" + other);
        assertThat(number("select verified_score from profile.skill_scores where user_id=? and skill_id=?", owner, java)).isEqualByComparingTo("82");
        assertThat(number("select self_score from profile.skill_scores where user_id=? and skill_id=?", owner, java)).isEqualByComparingTo("70");
        assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='SELF'", owner)).isEqualTo(2);
        assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='EXAM' and verified=true", owner)).isEqualTo(1);
        assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='SELF' and (verified=true or external_record_id is not null)", owner)).isZero();
        assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='SELF'", other)).isZero();
        // Repeated PUT replaces canonical evidence without duplicates.
        JsonNode updated = save("[" + entry(java, "99") + "]", "");
        assertThat(updated.size()).isEqualTo(1);
        assertThat(updated.get(0).path("verifiedScore").decimalValue()).isEqualByComparingTo("82");
        assertThat(updated.get(0).path("selfScore").decimalValue()).isEqualByComparingTo("99");
        save("[" + entry(java, "99") + "]", "");
        assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='SELF'", owner)).isEqualTo(1);
        assertThat(count("select count(*) from profile.skill_scores where user_id=? and skill_id=?", owner, english)).isZero();
    }
    @Test void clearingSelfKeepsVerifiedScoresAndRecomputePreservesSavedSelfScore() throws Exception {
        seedVerified(owner);
        save("[" + entry(java, "40") + "," + entry(english, "0") + "]", "");
        mvc.perform(post("/profiles/me/skills/recompute").header("Authorization", bearer)).andExpect(status().isOk());
        assertThat(number("select self_score from profile.skill_scores where user_id=? and skill_id=?", owner, java)).isEqualByComparingTo("40");
        JsonNode cleared = save("[]", "");
        assertThat(cleared.size()).isEqualTo(1);
        assertThat(cleared.get(0).path("selfScore").isNull()).isTrue();
        assertThat(cleared.get(0).path("verifiedScore").decimalValue()).isEqualByComparingTo("82");
        assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='SELF'", owner)).isZero();
        assertThat(count("select count(*) from profile.skill_scores where user_id=? and skill_id=?", owner, english)).isZero();
        mvc.perform(get("/profiles/me/skills").header("Authorization", bearer)).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].selfScore").isEmpty());
    }
    @Test void allInvalidRequestsRollBackTheWholeSelfCollection() throws Exception {
        save("[" + entry(java, "20") + "]", "");
        for (String body : new String[]{"[null]", "[{}]", "null", "{}",
            "[" + entry(java, "null") + "]", "[" + entry(java, "-1") + "]",
            "[" + entry(java, "100.000001") + "]", "[" + entry(java, "0.1234567") + "]",
            "[" + entry(java, "10") + "," + entry(java, "40") + "]",
            "[" + entry(java, "30") + "," + entry(UUID.randomUUID(), "10") + "]",
            "[{\"skillId\":\"not-a-uuid\",\"selfScore\":12}]"}) {
            mvc.perform(put("/profiles/me/self-skills").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
            assertThat(number("select self_score from profile.skill_scores where user_id=? and skill_id=?", owner, java)).isEqualByComparingTo("20");
            assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='SELF'", owner)).isEqualTo(1);
        }
    }
    @Test void onlyCandidateMayWriteAndPayloadCannotSetVerificationOrOwner() throws Exception {
        mvc.perform(put("/profiles/me/self-skills").contentType(MediaType.APPLICATION_JSON).content("[]"))
            .andExpect(status().isUnauthorized());
        for (Role role : Role.values()) {
            if (role == Role.CANDIDATE) continue;
            mvc.perform(put("/profiles/me/self-skills").header("Authorization", "Bearer " + jwt.generateJwtToken(owner, role, null))
                .contentType(MediaType.APPLICATION_JSON).content("[]")).andExpect(status().isForbidden());
        }
        JsonNode result = save("[{\"skillId\":\"" + java + "\",\"selfScore\":100,\"verified\":true,\"source\":\"EXAM\","
            + "\"verifiedScore\":100,\"userId\":\"" + UUID.randomUUID() + "\"}]", "");
        assertThat(result.get(0).path("verifiedScore").isNull()).isTrue();
        assertThat(result.get(0).path("evidences").get(0).path("source").asText()).isEqualTo("SELF");
        assertThat(result.get(0).path("evidences").get(0).path("verified").asBoolean()).isFalse();
    }
    @Test void clearsLegacySelfEvidenceAndSelfScoreEvenWhenTheyWereNotCanonical() throws Exception {
        jdbc.update("insert into profile.skill_evidences(id,user_id,skill_id,source,source_ref,title,score,weight,recorded_at,verified) "
            + "values (?,?,?,'SELF','hs01-demo-self-english','Demo',65,1,CURRENT_TIMESTAMP,false)", UUID.randomUUID(), owner, english);
        jdbc.update("insert into profile.skill_scores(user_id,skill_id,self_score,updated_at) values (?,?,65,CURRENT_TIMESTAMP)", owner, english);
        assertThat(save("[]", "").size()).isZero();
        assertThat(count("select count(*) from profile.skill_scores where user_id=?", owner)).isZero();
    }
    @Test void concurrentSelfReplacementsCommitMatchingEvidenceAndScore() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var a = executor.submit(() -> { start.await(); return save("[" + entry(java, "10") + "]", ""); });
            var b = executor.submit(() -> { start.await(); return save("[" + entry(english, "90") + "]", ""); });
            start.countDown(); a.get(); b.get();
        }
        assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='SELF'", owner)).isEqualTo(1);
        assertThat(count("select count(*) from profile.skill_scores where user_id=? and self_score is not null", owner)).isEqualTo(1);
        assertThat(count("select count(*) from profile.skill_scores s join profile.skill_evidences e on e.user_id=s.user_id and e.skill_id=s.skill_id "
            + "where s.user_id=? and e.source='SELF' and e.score=s.self_score", owner)).isEqualTo(1);
    }
    @Test void concurrentRecomputeAndSelfUpdateRetainVerifiedEvidenceAndSelfScore() throws Exception {
        seedVerified(owner);
        save("[" + entry(java, "20") + "]", "");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var update = executor.submit(() -> { start.await(); return save("[" + entry(java, "70") + "]", ""); });
            var recompute = executor.submit(() -> {
                start.await();
                return mvc.perform(post("/profiles/me/skills/recompute").header("Authorization", bearer))
                    .andExpect(status().isOk()).andReturn().getResponse().getStatus();
            });
            start.countDown(); update.get(); assertThat(recompute.get()).isEqualTo(200);
        }
        assertThat(number("select self_score from profile.skill_scores where user_id=? and skill_id=?", owner, java)).isEqualByComparingTo("70");
        assertThat(number("select verified_score from profile.skill_scores where user_id=? and skill_id=?", owner, java)).isEqualByComparingTo("82");
        assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='EXAM' and verified=true", owner)).isEqualTo(1);
        assertThat(count("select count(*) from profile.skill_evidences where user_id=? and source='SELF' and score=70 and verified=false", owner)).isEqualTo(1);
    }
    JsonNode save(String body, String query) throws Exception {
        return json.readTree(mvc.perform(put("/profiles/me/self-skills" + query).header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString());
    }
    String entry(UUID id, String score) { return "{\"skillId\":\"" + id + "\",\"selfScore\":" + score + "}"; }
    Integer count(String sql, Object... args) { return jdbc.queryForObject(sql, Integer.class, args); }
    BigDecimal number(String sql, Object... args) { return jdbc.queryForObject(sql, BigDecimal.class, args); }
    void seedVerified(UUID user) {
        jdbc.update("insert into profile.skill_evidences(id,user_id,skill_id,source,source_ref,title,score,weight,recorded_at,verified) "
            + "values (?,?,?,'EXAM','test-exam','Exam',82,1,CURRENT_TIMESTAMP,true)", UUID.randomUUID(), user, java);
        jdbc.update("insert into profile.skill_scores(user_id,skill_id,verified_score,level,updated_at) values (?,?,82,'GOOD',CURRENT_TIMESTAMP)", user, java);
    }
}
