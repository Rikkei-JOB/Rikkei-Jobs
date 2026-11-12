package com.rikkei.jobs.profile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.jwt.JwtProperties;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
    "spring.datasource.url=${HS01_TEST_DB_URL:jdbc:h2:mem:hs01_ping;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${HS01_TEST_DB_USER:sa}",
    "spring.datasource.password=${HS01_TEST_DB_PASSWORD:}",
    "logging.level.root=WARN",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret"
})
@AutoConfigureMockMvc
class ProfilePingControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;

    @Test
    void withoutTokenIs401InCommonErrorShape() throws Exception {
        mvc.perform(get("/profile/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void garbageTokenIs401() throws Exception {
        mvc.perform(get("/profile/ping").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenReachesControllerWithPrincipal() throws Exception {
        UUID user = UUID.randomUUID();
        String token = jwt.generateJwtToken(user, Role.MENTOR, null);

        mvc.perform(get("/profile/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("profile-service"))
                .andExpect(jsonPath("$.userId").value(user.toString()))
                .andExpect(jsonPath("$.role").value("MENTOR"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() throws Exception {
        JwtProperties other = new JwtProperties();
        other.setSecret("another_secret_another_secret_another_secret");
        String forged = new JwtUtils(other).generateJwtToken(UUID.randomUUID(), Role.ADMIN, null);

        mvc.perform(get("/profile/ping").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfAccountThatMustChangePasswordIs403() throws Exception {
        String token = jwt.generateJwtToken(UUID.randomUUID(), Role.HR, UUID.randomUUID(), true);

        mvc.perform(get("/profile/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MUST_CHANGE_PASSWORD"));
    }

    @Test
    void unknownRouteWithValidTokenIs404InCommonShape() throws Exception {
        String token = jwt.generateJwtToken(UUID.randomUUID(), Role.HR, null);

        mvc.perform(put("/profile/no-such-route/" + UUID.randomUUID()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void wrongMethodIs405InCommonShape() throws Exception {
        String token = jwt.generateJwtToken(UUID.randomUUID(), Role.HR, null);

        mvc.perform(post("/profile/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }
}
