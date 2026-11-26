package com.rikkei.jobs.profile;

import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.net.URI;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:dn06_verified_skills;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa", "spring.datasource.password=",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret",
    "app.profile.internal-service-key=skills-test",
    "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
class InternalVerifiedSkillsApiTest {
    static final String JAVA = "00000000-0000-0000-0001-000000000001";
    static final String COMMUNICATION = "00000000-0000-0000-0001-000000000018";

    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired JdbcTemplate jdbc;

    private String bearer() {
        return "Bearer " + jwt.generateJwtToken(UUID.randomUUID(), Role.CANDIDATE, null);
    }

    @Test void returnsOnlyVerifiedScores() throws Exception {
        UUID user = UUID.randomUUID();
        jdbc.update("insert into profile.skill_scores(user_id, skill_id, verified_score, self_score, level, updated_at) "
            + "values (?, ?, 82.058824, null, 'EXCELLENT', current_timestamp)", user, UUID.fromString(JAVA));
        jdbc.update("insert into profile.skill_scores(user_id, skill_id, verified_score, self_score, level, updated_at) "
            + "values (?, ?, null, 65, null, current_timestamp)", user, UUID.fromString(COMMUNICATION));

        mvc.perform(get("/internal/profiles/" + user + "/verified-skills")
                .header("Authorization", bearer()).header("X-Internal-Key", "skills-test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].skillId").value(JAVA))
            .andExpect(jsonPath("$[0].verifiedScore").value(82.058824))
            .andExpect(jsonPath("$[0].updatedAt").exists());
    }

    @Test void noScoresGivesEmptyArray() throws Exception {
        mvc.perform(get("/internal/profiles/" + UUID.randomUUID() + "/verified-skills")
                .header("Authorization", bearer()).header("X-Internal-Key", "skills-test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test void wrongOrMissingKeyIs401() throws Exception {
        String path = "/internal/profiles/" + UUID.randomUUID() + "/verified-skills";
        mvc.perform(get(path).header("Authorization", bearer()).header("X-Internal-Key", "wrong"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get(path).header("Authorization", bearer()))
            .andExpect(status().isUnauthorized());
    }

    @Test void missingJwtIs401() throws Exception {
        mvc.perform(get("/internal/profiles/" + UUID.randomUUID() + "/verified-skills")
                .header("X-Internal-Key", "skills-test"))
            .andExpect(status().isUnauthorized());
    }

    @Test void encodedPathWithoutKeyIsRejected() throws Exception {
        mvc.perform(get(URI.create("/%69nternal/profiles/" + UUID.randomUUID() + "/verified-skills"))
                .header("Authorization", bearer()))
            .andExpect(status().is4xxClientError());
    }
}
