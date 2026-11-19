package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.principle.Role;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestPropertySource(properties = "app.recruitment.internal-service-key=apps-test")
class InternalApplicationExistsApiTest extends ApplicationIntegrationSupport {
    private static final String KEY = "apps-test";

    @Test void appliedIsTrueForAnyStatus() throws Exception {
        apply();
        mvc.perform(get("/internal/applications/exists").param("jobId", job.toString()).param("userId", user.toString())
            .header("Authorization", hr()).header("X-Internal-Key", KEY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.applied").value(true));
        jdbc.update("update recruitment.applications set status='WITHDRAWN'");
        mvc.perform(get("/internal/applications/exists").param("jobId", job.toString()).param("userId", user.toString())
            .header("Authorization", hr()).header("X-Internal-Key", KEY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.applied").value(true));
    }

    @Test void notAppliedIsFalse() throws Exception {
        apply();
        mvc.perform(get("/internal/applications/exists").param("jobId", job.toString()).param("userId", UUID.randomUUID().toString())
            .header("Authorization", staff()).header("X-Internal-Key", KEY))
            .andExpect(status().isOk()).andExpect(jsonPath("$.applied").value(false));
    }

    @Test void wrongOrMissingKeyIs401() throws Exception {
        mvc.perform(get("/internal/applications/exists").param("jobId", job.toString()).param("userId", user.toString())
            .header("Authorization", hr()).header("X-Internal-Key", "wrong")).andExpect(status().isUnauthorized());
        mvc.perform(get("/internal/applications/exists").param("jobId", job.toString()).param("userId", user.toString())
            .header("Authorization", hr())).andExpect(status().isUnauthorized());
    }

    @Test void candidateTokenIs403() throws Exception {
        mvc.perform(get("/internal/applications/exists").param("jobId", job.toString()).param("userId", user.toString())
            .header("Authorization", candidate()).header("X-Internal-Key", KEY)).andExpect(status().isForbidden());
    }

    @Test void missingParamIs400() throws Exception {
        mvc.perform(get("/internal/applications/exists").param("jobId", job.toString())
            .header("Authorization", hr()).header("X-Internal-Key", KEY))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private String hr() { return token(UUID.randomUUID(), Role.HR, company); }
    private String staff() { return token(UUID.randomUUID(), Role.STAFF, null); }
}
