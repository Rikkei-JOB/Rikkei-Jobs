package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.client.JobApplicationClient;
import com.rikkei.jobs.recruitment.client.ProfileCvClient;
import com.rikkei.jobs.recruitment.dto.integration.CvPdf;
import com.rikkei.jobs.recruitment.dto.integration.JobApplicationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"app.recruitment.outbox-enabled=false", "app.jwt.secret=test_secret_test_secret_test_secret_test_secret", "app.recruitment.cv-storage-dir=target/ut01-snapshots"})
@AutoConfigureMockMvc
abstract class ApplicationIntegrationSupport {
    private static PostgreSQLContainer<?> postgres;
    static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @MockBean JobApplicationClient jobs;
    @MockBean ProfileCvClient cvs;
    @MockBean Clock clock;
    UUID user = UUID.randomUUID(), job = UUID.randomUUID(), company = UUID.randomUUID(), cv = UUID.randomUUID();

    @DynamicPropertySource static synchronized void database(DynamicPropertyRegistry properties) {
        String url = System.getenv("UT01_TEST_DB_URL");
        if (url == null || url.isBlank()) {
            if (postgres == null) { postgres = new PostgreSQLContainer<>("postgres:16-alpine"); postgres.start(); }
            properties.add("spring.datasource.url", postgres::getJdbcUrl);
            properties.add("spring.datasource.username", postgres::getUsername);
            properties.add("spring.datasource.password", postgres::getPassword);
        } else {
            properties.add("spring.datasource.url", () -> url);
            properties.add("spring.datasource.username", () -> System.getenv().getOrDefault("UT01_TEST_DB_USER", "postgres"));
            properties.add("spring.datasource.password", () -> System.getenv().getOrDefault("UT01_TEST_DB_PASSWORD", ""));
        }
    }

    @BeforeEach void resetData() {
        jdbc.update("delete from recruitment.application_notification_outbox");
        jdbc.update("delete from recruitment.application_status_history");
        jdbc.update("delete from recruitment.applications");
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(jobs.getContext(job)).thenReturn(context("APPROVED", NOW.plusSeconds(3600), false));
        when(cvs.exportOwnedPdf(eq(cv), anyString())).thenReturn(new CvPdf("%PDF-1.7\noriginal-cv".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    JobApplicationContext context(String status, Instant deadline, boolean locked) {
        return new JobApplicationContext(job, company, "Java Developer", "Company A", status, deadline, locked, List.of("hr@company.test", "hr@company.test"));
    }
    String token(UUID id, Role role, UUID companyId) { return "Bearer " + jwt.generateJwtToken(id, role, companyId); }
    String candidate() { return token(user, Role.CANDIDATE, null); }
    String body() { return "{\"cvId\":\"" + cv + "\",\"coverLetter\":\"Xin chào\"}"; }
    UUID apply() throws Exception {
        String response = mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate())
            .contentType("application/json").content(body())).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").asText());
    }
    int count(String table) { return jdbc.queryForObject("select count(*) from recruitment." + table, Integer.class); }
}
