package com.rikkei.jobs.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=${HS02_TEST_DB_URL:jdbc:h2:mem:hs02_api;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
    "spring.datasource.username=${HS02_TEST_DB_USER:sa}", "spring.datasource.password=${HS02_TEST_DB_PASSWORD:}",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret",
    "app.profile.identity-service-key=identity-test", "app.profile.lms-service-key=lms-test",
    "logging.level.root=WARN", "debug=false"
})
@AutoConfigureMockMvc
class CandidateProfileApiTest {
    static final HttpServer upstream;
    static final AtomicInteger calls = new AtomicInteger();
    static volatile int upstreamStatus = 200;
    static volatile String student;
    static volatile String identity;
    static volatile boolean correctKeys;
    static {
        try {
            upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            upstream.createContext("/", exchange -> {
                calls.incrementAndGet();
                String path = exchange.getRequestURI().getPath();
                boolean internal = path.startsWith("/internal/users/");
                correctKeys &= internal
                    ? "identity-test".equals(exchange.getRequestHeaders().getFirst("X-Internal-Key"))
                    : "lms-test".equals(exchange.getRequestHeaders().getFirst("X-Service-Key"));
                String body = internal ? identity : student;
                int status = !internal && !path.equals("/lms/students/lms-sv-001") ? 404 : upstreamStatus;
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            upstream.start();
        } catch (Exception ex) { throw new ExceptionInInitializerError(ex); }
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        String url = "http://127.0.0.1:" + upstream.getAddress().getPort();
        registry.add("app.profile.identity-base-url", () -> url);
        registry.add("app.profile.lms-base-url", () -> url);
    }
    @AfterAll static void stop() { upstream.stop(0); }
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    UUID owner;
    String token;

    @BeforeEach void setup() {
        owner = UUID.randomUUID();
        token = jwt.generateJwtToken(owner, Role.CANDIDATE, null);
        calls.set(0); correctKeys = true; upstreamStatus = 200;
        identity = "{\"userId\":\"" + owner + "\",\"lmsUserId\":\"lms-sv-001\"}";
        student = "{\"lmsUserId\":\"lms-sv-001\",\"fullName\":\"Nguyễn Văn An\","
            + "\"email\":\"candidate1@rikkeijobs.test\",\"program\":\"Kỹ sư phần mềm\","
            + "\"classCode\":\"KS25-CNTT1\",\"courseName\":\"K25\"}";
    }
    @Test void createsPrivateProfileFromLmsOnceAndReadsItOffline() throws Exception {
        mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(owner.toString()))
            .andExpect(jsonPath("$.fullName").value("Nguyễn Văn An"))
            .andExpect(jsonPath("$.program").value("Kỹ sư phần mềm"))
            .andExpect(jsonPath("$.classCode").value("KS25-CNTT1"))
            .andExpect(jsonPath("$.courseName").value("K25"))
            .andExpect(jsonPath("$.visibility").value("PRIVATE"));
        assertThat(correctKeys).isTrue();
        assertThat(calls.get()).isEqualTo(2);
        upstreamStatus = 503;
        mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        assertThat(calls.get()).isEqualTo(2);
    }
    @Test void updatesOnlyEditableFieldsAndDoesNotTouchAnotherOwner() throws Exception {
        UUID other = UUID.randomUUID();
        mvc.perform(put("/profiles/me?userId=" + other).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.ofEntries(
                Map.entry("fullName", "  An mới  "), Map.entry("email", "an@example.com"),
                Map.entry("phone", "0912345678"), Map.entry("dob", "2000-02-29"),
                Map.entry("gender", "MALE"), Map.entry("headline", "Lập trình viên"),
                Map.entry("userId", other.toString()), Map.entry("program", "HACKED"),
                Map.entry("classCode", "HACKED"), Map.entry("courseName", "HACKED"),
                Map.entry("visibility", "PUBLIC"), Map.entry("avatarUrl", "https://evil.example/a.png")))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("An mới"))
            .andExpect(jsonPath("$.dob").value("2000-02-29"))
            .andExpect(jsonPath("$.classCode").value("KS25-CNTT1"))
            .andExpect(jsonPath("$.program").value("Kỹ sư phần mềm"))
            .andExpect(jsonPath("$.courseName").value("K25"))
            .andExpect(jsonPath("$.visibility").value("PRIVATE"))
            .andExpect(jsonPath("$.avatarUrl").isEmpty());
        assertThat(jdbc.queryForObject("select count(*) from profile.candidate_profiles where user_id=?", Integer.class, other)).isZero();
        mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + token))
            .andExpect(jsonPath("$.email").value("an@example.com"));
    }
    @Test void validatesPersonalDataBeforeAnyDependencyCall() throws Exception {
        for (String fields : new String[]{"\"fullName\":\"\",\"email\":\"a@example.com\"",
            "\"fullName\":\"An\",\"email\":\"invalid\"",
            "\"fullName\":\"An\",\"email\":\"a@example.com\",\"phone\":\"1234567890\"",
            "\"fullName\":\"An\",\"email\":\"a@example.com\",\"dob\":\"2999-01-01\"",
            "\"fullName\":\"An\",\"email\":\"a@example.com\",\"dob\":\"2001-02-29\"",
            "\"fullName\":\"An\",\"email\":\"a@example.com\",\"gender\":\"INVALID\""}) {
            mvc.perform(put("/profiles/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{" + fields + "}"))
                .andExpect(status().isBadRequest());
        }
        assertThat(calls.get()).isZero();
    }
    @Test void rejectsUnauthenticatedAndOtherRoles() throws Exception {
        mvc.perform(get("/profiles/me")).andExpect(status().isUnauthorized());
        for (Role role : Role.values()) {
            if (role == Role.CANDIDATE) continue;
            String otherToken = jwt.generateJwtToken(UUID.randomUUID(), role, null);
            mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + otherToken)).andExpect(status().isForbidden());
            mvc.perform(put("/profiles/me").header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"fullName\":\"An\",\"email\":\"a@example.com\"}"))
                .andExpect(status().isForbidden());
        }
        assertThat(calls.get()).isZero();
    }
    @Test void unavailableDependencyDoesNotLeavePartialProfileAndCanRetry() throws Exception {
        upstreamStatus = 503;
        mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("EXTERNAL_UNAVAILABLE"));
        assertThat(jdbc.queryForObject("select count(*) from profile.candidate_profiles where user_id=?", Integer.class, owner)).isZero();
        upstreamStatus = 200;
        mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }
    @Test void incompleteOrMismatchedExternalDataIsRejected() throws Exception {
        for (String bad : new String[]{"{}", student.replace("lms-sv-001", "lms-sv-999")}) {
            student = bad;
            mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("EXTERNAL_INVALID_RESPONSE"));
        }
        assertThat(jdbc.queryForObject("select count(*) from profile.candidate_profiles where user_id=?", Integer.class, owner)).isZero();
    }
    @Test void missingIdentityReturnsNotFound() throws Exception {
        upstreamStatus = 404;
        mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + token)).andExpect(status().isNotFound());
    }
    @Test void concurrentFirstRequestsCreateOneProfile() throws Exception {
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var latch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<Integer> read = () -> {
                latch.await();
                return mvc.perform(get("/profiles/me").header("Authorization", "Bearer " + token))
                    .andReturn().getResponse().getStatus();
            };
            var first = executor.submit(read); var second = executor.submit(read); latch.countDown();
            assertThat(first.get()).isEqualTo(200); assertThat(second.get()).isEqualTo(200);
        }
        assertThat(jdbc.queryForObject("select count(*) from profile.candidate_profiles where user_id=?", Integer.class, owner)).isEqualTo(1);
        assertThat(calls.get()).isEqualTo(2);
    }
}
