package com.rikkei.jobs.assessment;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret"
})
@AutoConfigureMockMvc
class AssessmentPingControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;

    @Test
    void withoutTokenIs401InCommonErrorShape() throws Exception {
        mvc.perform(get("/assessment/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void garbageTokenIs401() throws Exception {
        mvc.perform(get("/assessment/ping").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenReachesControllerWithPrincipal() throws Exception {
        UUID user = UUID.randomUUID();
        String token = jwt.generateJwtToken(user, Role.MENTOR, null);

        mvc.perform(get("/assessment/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("assessment-service"))
                .andExpect(jsonPath("$.userId").value(user.toString()))
                .andExpect(jsonPath("$.role").value("MENTOR"));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() throws Exception {
        JwtProperties other = new JwtProperties();
        other.setSecret("another_secret_another_secret_another_secret");
        String forged = new JwtUtils(other).generateJwtToken(UUID.randomUUID(), Role.ADMIN, null);

        mvc.perform(get("/assessment/ping").header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfAccountThatMustChangePasswordIs403() throws Exception {
        String token = jwt.generateJwtToken(UUID.randomUUID(), Role.HR, UUID.randomUUID(), true);

        mvc.perform(get("/assessment/ping").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MUST_CHANGE_PASSWORD"));
    }
}
