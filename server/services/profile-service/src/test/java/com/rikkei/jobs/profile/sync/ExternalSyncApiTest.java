package com.rikkei.jobs.profile.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.profile.dto.SkillProfileView;
import com.rikkei.jobs.profile.entity.SyncRun;
import com.rikkei.jobs.profile.entity.SyncStatus;
import com.rikkei.jobs.profile.entity.SyncTrigger;
import com.rikkei.jobs.profile.repository.SyncRunRepository;
import com.rikkei.jobs.profile.service.SkillProfileService;
import com.rikkei.jobs.profile.service.sync.SyncService;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Tiêu chí nghiệm thu NT-05 trên API thật, với một HTTP server đóng vai mock (đổi được dữ liệu, tắt được).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:nt05_sync;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.jwt.secret=test_secret_test_secret_test_secret_test_secret",
        "app.sync.retry-backoff=0ms",
        "app.sync.nightly-cron=-",
        "logging.level.root=WARN"
})
@AutoConfigureMockMvc
class ExternalSyncApiTest {
    static final UUID CANDIDATE1 = UUID.fromString("50000000-0000-0000-0000-000000000001");
    static final UUID CANDIDATE2 = UUID.fromString("50000000-0000-0000-0000-000000000002");
    static final UUID NO_LMS = UUID.fromString("50000000-0000-0000-0000-000000000003");
    static final UUID HR = UUID.fromString("50000000-0000-0000-0000-000000000004");
    static final UUID STAFF = UUID.fromString("50000000-0000-0000-0000-000000000005");
    static final String LMS1 = "lms-sv-001";

    static final Map<String, List<Map<String, Object>>> DATA = new ConcurrentHashMap<>();
    static final Map<String, AtomicInteger> HITS = new ConcurrentHashMap<>();
    static final AtomicBoolean DOWN = new AtomicBoolean();
    static final HttpServer MOCK = mockServer();

    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired SyncRunRepository runs;
    @Autowired SyncService sync;
    @Autowired SkillProfileService skills;

    @DynamicPropertySource
    static void mockAddress(DynamicPropertyRegistry registry) {
        registry.add("app.sync.base-url", () -> "http://localhost:" + MOCK.getAddress().getPort());
    }

    @AfterAll
    static void stopMock() {
        MOCK.stop(0);
    }

    @BeforeEach
    void resetState() {
        DOWN.set(false);
        HITS.clear();
        DATA.clear();
        DATA.put("/lms/students/" + LMS1 + "/results", new ArrayList<>(List.of(
                lms("JAVA01", "Lập trình Java", 8.5), lms("SQL01", "Cơ sở dữ liệu", 8.0))));
        DATA.put("/exam/students/" + LMS1 + "/results", new ArrayList<>(List.of(Map.of(
                "examCode", "EX-JAVA-2026-10", "subjectCode", "JAVA01", "score", 8.0,
                "takenAt", "2026-10-20T08:00:00+07:00"))));
        DATA.put("/pm/students/" + LMS1 + "/internships", new ArrayList<>(List.of(Map.of(
                "batchId", "IOC-JB-2026-09", "batchName", "IOC Java Backend 09/2026",
                "topicName", "Hệ thống đặt lịch phòng họp", "endDate", "2026-10-30", "finalScore", 8.5,
                "tasks", List.of(Map.of("title", "API đặt phòng", "acceptedScore", 9,
                        "acceptedAt", "2026-10-23T17:00:00+07:00"))))));

        jdbc.execute("CREATE TABLE IF NOT EXISTS public.users (id UUID PRIMARY KEY, email VARCHAR(255) NOT NULL, "
                + "full_name VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL, lms_user_id VARCHAR(100) UNIQUE, "
                + "status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE')");
        jdbc.update("DELETE FROM public.users");
        user(CANDIDATE1, "candidate1@rikkeijobs.test", "Nguyễn Văn An", "CANDIDATE", LMS1);
        user(CANDIDATE2, "candidate2@rikkeijobs.test", "Trần Thị Bình", "CANDIDATE", "lms-sv-002");
        user(NO_LMS, "candidate9@rikkeijobs.test", "Lê Văn Chưa Liên Kết", "CANDIDATE", null);
        user(HR, "hr1@rikkeijobs.test", "HR Một", "HR", null);

        for (String table : List.of("manual_overrides", "sync_runs", "skill_evidences", "skill_scores",
                "external_records", "skill_profile_states")) {
            jdbc.update("DELETE FROM profile." + table);
        }
    }

    // ---------- Tiêu chí: đổi điểm trong mock, Đồng bộ lại → external_records có điểm mới ----------

    @Test
    void manualSyncStoresAllThreeSourcesAndRecomputesSkills() throws Exception {
        JsonNode run = syncNow(CANDIDATE1);

        assertThat(run.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(run.path("trigger").asText()).isEqualTo("MANUAL");
        assertThat(run.path("triggeredBy").asText()).isEqualTo(STAFF.toString());
        assertThat(run.path("candidateName").asText()).isEqualTo("Nguyễn Văn An");
        assertThat(run.path("counts").path("fetched").asInt()).isEqualTo(4);
        assertThat(run.path("counts").path("inserted").asInt()).isEqualTo(4);
        assertThat(sourceKeys()).containsExactlyInAnyOrder("LMS|lms-sv-001:JAVA01", "LMS|lms-sv-001:SQL01",
                "EXAM|lms-sv-001:EX-JAVA-2026-10", "PM|lms-sv-001:IOC-JB-2026-09");
        // Kỹ năng HS-01 được tính lại từ bản ghi vừa đồng bộ (JAVA01 LMS 85, thi Java 80).
        assertThat(javaScore()).isNotNull();
        assertThat(skill("REST_API")).as("đợt thực tập ánh xạ sang REST API").isNotNull();
    }

    @Test
    void changedScoreInMockIsStoredOnResync() throws Exception {
        syncNow(CANDIDATE1);
        BigDecimal before = javaScore();

        setLmsScore("JAVA01", 9.5);
        JsonNode run = syncNow(CANDIDATE1);

        assertThat(run.path("counts").path("updated").asInt()).isEqualTo(1);
        assertThat(run.path("counts").path("unchanged").asInt()).isEqualTo(3);
        assertThat(score("lms-sv-001:JAVA01")).isEqualByComparingTo("9.5");
        assertThat(javaScore()).isGreaterThan(before);
    }

    // ---------- Tiêu chí: đồng bộ 2 lần liên tiếp không sinh bản ghi trùng ----------

    @Test
    void syncingTwiceCreatesNoDuplicates() throws Exception {
        syncNow(CANDIDATE1);
        JsonNode second = syncNow(CANDIDATE1);

        assertThat(second.path("counts").path("inserted").asInt()).isZero();
        assertThat(second.path("counts").path("unchanged").asInt()).isEqualTo(4);
        assertThat(count("external_records")).isEqualTo(4);
        assertThat(count("sync_runs")).isEqualTo(2);
    }

    // ---------- Tiêu chí: tắt mock → FAILED kèm lỗi, dữ liệu cũ còn nguyên ----------

    @Test
    void mockDownRecordsFailedRunAndKeepsOldData() throws Exception {
        syncNow(CANDIDATE1);
        setLmsScore("JAVA01", 2.0);
        DOWN.set(true);

        JsonNode run = syncNow(CANDIDATE1);

        assertThat(run.path("status").asText()).isEqualTo("FAILED");
        assertThat(run.path("error").asText()).contains("Không kết nối được LMS").contains("đã thử 3 lần");
        assertThat(run.path("counts").isNull()).isTrue();
        // Số lần thử lại kiểm chính xác ở ExternalSourceClientTest (HttpClient của JDK tự gửi lại GET khi kết nối bị cắt).
        assertThat(HITS.get("/lms/students/" + LMS1 + "/results").get()).isGreaterThanOrEqualTo(4);
        assertThat(HITS.get("/exam/students/" + LMS1 + "/results").get())
                .as("LMS lỗi thì dừng, không gọi tiếp Khảo thí").isEqualTo(1);
        assertThat(count("external_records")).isEqualTo(4);
        assertThat(score("lms-sv-001:JAVA01")).isEqualByComparingTo("8.5");

        mvc.perform(get("/staff/sync-runs?status=FAILED").header("Authorization", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].error").value(run.path("error").asText()));
    }

    @Test
    void oneSourceFailingWritesNothing() throws Exception {
        DATA.remove("/pm/students/" + LMS1 + "/internships"); // stub trả 500 cho đường dẫn không có dữ liệu

        JsonNode run = syncNow(CANDIDATE1);

        assertThat(run.path("status").asText()).isEqualTo("FAILED");
        assertThat(run.path("error").asText()).contains("lms-pm trả lỗi HTTP 500");
        assertThat(count("external_records")).isZero();
    }

    // ---------- Tiêu chí: sửa tay 1 trường → đồng bộ lại không ghi đè trường đó ----------

    @Test
    void manualOverrideSurvivesResyncWhileOtherFieldsStillUpdate() throws Exception {
        syncNow(CANDIDATE1);
        UUID recordId = recordId("lms-sv-001:JAVA01");

        override(recordId, "score", "9", "Điểm phúc khảo")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(9))
                .andExpect(jsonPath("$.overriddenFields[0]").value("score"))
                .andExpect(jsonPath("$.overrides[0].reason").value("Điểm phúc khảo"))
                .andExpect(jsonPath("$.overrides[0].staffId").value(STAFF.toString()));
        BigDecimal afterOverride = javaScore();

        setLmsScore("JAVA01", 6.0);
        renameLmsSubject("JAVA01", "Lập trình Java nâng cao");
        syncNow(CANDIDATE1);

        assertThat(score("lms-sv-001:JAVA01")).isEqualByComparingTo("9");
        assertThat(jdbc.queryForObject("SELECT title FROM profile.external_records WHERE source_key = ?",
                String.class, "lms-sv-001:JAVA01")).isEqualTo("Lập trình Java nâng cao");
        assertThat(javaScore()).isEqualByComparingTo(afterOverride);

        JsonNode records = body(mvc.perform(get("/staff/external-records?userId=" + CANDIDATE1)
                .header("Authorization", staff())).andExpect(status().isOk()));
        JsonNode java = find(records, "sourceKey", "lms-sv-001:JAVA01");
        assertThat(java.path("payload").path("score").decimalValue())
                .as("payload vẫn giữ giá trị nguồn mới để cán bộ so sánh").isEqualByComparingTo("6.0");
        assertThat(java.path("score").decimalValue()).isEqualByComparingTo("9");
    }

    @Test
    void overrideValidatesFieldValueAndReason() throws Exception {
        syncNow(CANDIDATE1);
        UUID recordId = recordId("lms-sv-001:JAVA01");

        override(recordId, "verified", "true", "x").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.field").exists());
        override(recordId, "score", "11", "x").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.value").value("Điểm phải từ 0 đến 10"));
        override(recordId, "score", "abc", "x").andExpect(status().isBadRequest());
        override(recordId, "score", "9", " ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
        override(UUID.randomUUID(), "score", "9", "x").andExpect(status().isNotFound());
        assertThat(count("manual_overrides")).isZero();
    }

    // ---------- Phân quyền và lỗi đầu vào ----------

    @Test
    void onlyStaffCanUseSyncApis() throws Exception {
        mvc.perform(post("/staff/sync/" + CANDIDATE1)).andExpect(status().isUnauthorized());
        mvc.perform(post("/staff/sync/" + CANDIDATE1).header("Authorization", bearer(CANDIDATE1, Role.CANDIDATE)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/staff/sync-runs").header("Authorization", bearer(HR, Role.HR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/staff/external-records?userId=" + CANDIDATE1)
                        .header("Authorization", bearer(UUID.randomUUID(), Role.MENTOR)))
                .andExpect(status().isForbidden());
        assertThat(count("sync_runs")).isZero();
    }

    @Test
    void unknownCandidateIs404AndCandidateWithoutLmsIs400() throws Exception {
        mvc.perform(post("/staff/sync/" + UUID.randomUUID()).header("Authorization", staff()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/staff/sync/" + HR).header("Authorization", staff()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/staff/sync/" + NO_LMS).header("Authorization", staff()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void runListFiltersAndValidatesPaging() throws Exception {
        syncNow(CANDIDATE1);
        DOWN.set(true);
        syncNow(CANDIDATE1);

        mvc.perform(get("/staff/sync-runs").header("Authorization", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].status").value("FAILED"));
        mvc.perform(get("/staff/sync-runs?status=SUCCESS&userId=" + CANDIDATE1).header("Authorization", staff()))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].lmsUserId").value(LMS1));
        mvc.perform(get("/staff/sync-runs?userId=" + CANDIDATE2).header("Authorization", staff()))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/staff/sync-runs?page=0").header("Authorization", staff()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/staff/sync-runs?size=101").header("Authorization", staff()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void candidateSearchShowsLastRun() throws Exception {
        syncNow(CANDIDATE1);

        JsonNode found = body(mvc.perform(get("/staff/sync/candidates?q=VĂN AN").header("Authorization", staff()))
                .andExpect(status().isOk()));
        assertThat(found).hasSize(1);
        assertThat(found.get(0).path("lmsUserId").asText()).isEqualTo(LMS1);
        assertThat(found.get(0).path("lastRun").path("status").asText()).isEqualTo("SUCCESS");

        JsonNode byLms = body(mvc.perform(get("/staff/sync/candidates?q=lms-sv-002").header("Authorization", staff())));
        assertThat(byLms.get(0).path("lastRun").isNull()).isTrue();
        JsonNode all = body(mvc.perform(get("/staff/sync/candidates").header("Authorization", staff())));
        assertThat(all).as("chỉ ứng viên, không có HR").hasSize(3);
        assertThat(body(mvc.perform(get("/staff/sync/candidates?q=%25").header("Authorization", staff()))))
                .as("ký tự % được tìm đúng nghĩa đen").isEmpty();
    }

    @Test
    void invalidSourceItemsAreSkippedNotFatal() throws Exception {
        DATA.get("/lms/students/" + LMS1 + "/results").add(lms("BAD01", "Điểm sai thang", 12));
        DATA.get("/lms/students/" + LMS1 + "/results").add(Map.of("subjectName", "Thiếu mã", "score", 7));

        JsonNode run = syncNow(CANDIDATE1);

        assertThat(run.path("status").asText()).isEqualTo("SUCCESS");
        assertThat(run.path("counts").path("fetched").asInt()).isEqualTo(6);
        assertThat(run.path("counts").path("skipped").asInt()).isEqualTo(2);
        assertThat(count("external_records")).isEqualTo(4);
    }

    // ---------- Đồng bộ nền khi mở hồ sơ và job đêm ----------

    @Test
    void openingOwnProfileSyncsInBackgroundOnlyWhenStale() throws Exception {
        mvc.perform(get("/profiles/me/skills").header("Authorization", bearer(CANDIDATE1, Role.CANDIDATE)))
                .andExpect(status().isOk());
        SyncRun run = awaitRuns(1).getFirst();
        assertThat(run.getTrigger()).isEqualTo(SyncTrigger.ON_OPEN);
        assertThat(run.getStatus()).isEqualTo(SyncStatus.SUCCESS);

        mvc.perform(get("/profiles/me/skills").header("Authorization", bearer(CANDIDATE1, Role.CANDIDATE)))
                .andExpect(status().isOk());
        Thread.sleep(300);
        assertThat(runs.count()).as("lần cuối chưa quá 1 giờ thì không đồng bộ lại").isEqualTo(1);

        jdbc.update("UPDATE profile.sync_runs SET started_at = started_at - INTERVAL '2' HOUR");
        mvc.perform(get("/profiles/me/skills").header("Authorization", bearer(CANDIDATE1, Role.CANDIDATE)))
                .andExpect(status().isOk());
        awaitRuns(2);
    }

    @Test
    void nightlyJobSyncsEveryLinkedActiveCandidate() {
        List<SyncRun> result = sync.runNightly(null);

        assertThat(result).extracting(SyncRun::getUserId).containsExactly(CANDIDATE1, CANDIDATE2);
        assertThat(result).allMatch(r -> r.getTrigger() == SyncTrigger.NIGHTLY);
        // candidate2 không có dữ liệu trên stub (500) → FAILED, nhưng không làm hỏng lần chạy của candidate1.
        assertThat(result.get(0).getStatus()).isEqualTo(SyncStatus.SUCCESS);
        assertThat(result.get(1).getStatus()).isEqualTo(SyncStatus.FAILED);
    }

    @Test
    void nightlyJobCanBeStartedByStaff() throws Exception {
        mvc.perform(post("/staff/sync/nightly").header("Authorization", staff()))
                .andExpect(status().isAccepted());
        assertThat(awaitRuns(2)).allMatch(r -> STAFF.equals(r.getTriggeredBy()));
    }

    @Test
    void apiDocsListNewEndpoints() throws Exception {
        mvc.perform(get("/profile/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/staff/sync/{userId}'].post").exists())
                .andExpect(jsonPath("$.paths['/staff/sync-runs'].get").exists())
                .andExpect(jsonPath("$.paths['/staff/external-records/{id}/override'].put").exists());
    }

    // ---------- Hỗ trợ ----------

    JsonNode syncNow(UUID userId) throws Exception {
        return body(mvc.perform(post("/staff/sync/" + userId).header("Authorization", staff()))
                .andExpect(status().isOk()));
    }

    ResultActions override(UUID recordId, String field, String value, String reason) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("field", field);
        body.put("value", value);
        body.put("reason", reason);
        return mvc.perform(put("/staff/external-records/" + recordId + "/override")
                .header("Authorization", staff())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    List<SyncRun> awaitRuns(int expected) throws InterruptedException {
        for (int i = 0; i < 100 && runs.count() < expected; i++) {
            Thread.sleep(50);
        }
        List<SyncRun> all = runs.findAll();
        assertThat(all).hasSize(expected);
        return all;
    }

    BigDecimal javaScore() {
        SkillProfileView java = skill("JAVA");
        return java == null ? null : java.verifiedScore();
    }

    SkillProfileView skill(String code) {
        return skills.getSkills(CANDIDATE1).stream().filter(s -> s.skill().code().equals(code)).findFirst().orElse(null);
    }

    List<String> sourceKeys() {
        return jdbc.queryForList("SELECT source || '|' || source_key FROM profile.external_records", String.class);
    }

    BigDecimal score(String sourceKey) {
        return jdbc.queryForObject("SELECT score FROM profile.external_records WHERE source_key = ?",
                BigDecimal.class, sourceKey);
    }

    UUID recordId(String sourceKey) {
        return jdbc.queryForObject("SELECT id FROM profile.external_records WHERE source_key = ?", UUID.class, sourceKey);
    }

    long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM profile." + table, Long.class);
    }

    void setLmsScore(String subjectCode, double score) {
        replaceLms(subjectCode, "score", score);
    }

    void renameLmsSubject(String subjectCode, String name) {
        replaceLms(subjectCode, "subjectName", name);
    }

    void replaceLms(String subjectCode, String field, Object value) {
        List<Map<String, Object>> items = DATA.get("/lms/students/" + LMS1 + "/results");
        for (int i = 0; i < items.size(); i++) {
            if (subjectCode.equals(items.get(i).get("subjectCode"))) {
                Map<String, Object> copy = new LinkedHashMap<>(items.get(i));
                copy.put(field, value);
                items.set(i, copy);
            }
        }
    }

    static JsonNode find(JsonNode array, String field, String value) {
        for (JsonNode item : array) {
            if (value.equals(item.path(field).asText())) {
                return item;
            }
        }
        throw new AssertionError("Không thấy " + field + "=" + value);
    }

    JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    String staff() {
        return bearer(STAFF, Role.STAFF);
    }

    String bearer(UUID userId, Role role) {
        return "Bearer " + jwt.generateJwtToken(userId, role, null);
    }

    void user(UUID id, String email, String name, String role, String lmsUserId) {
        jdbc.update("INSERT INTO public.users(id, email, full_name, role, lms_user_id) VALUES (?, ?, ?, ?, ?)",
                id, email, name, role, lmsUserId);
    }

    static Map<String, Object> lms(String code, String name, double score) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("subjectCode", code);
        item.put("subjectName", name);
        item.put("score", score);
        item.put("attendanceRate", 95);
        item.put("homeworkRate", 90);
        item.put("rpoint", 120);
        return item;
    }

    /** Giả lập mock: kiểm X-Service-Key, trả dữ liệu theo đường dẫn, 500 khi không có; DOWN thì cắt kết nối. */
    static HttpServer mockServer() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                HITS.computeIfAbsent(path, p -> new AtomicInteger()).incrementAndGet();
                if (DOWN.get()) {
                    exchange.close(); // đóng kết nối không trả gì, như khi mock tắt giữa chừng
                    return;
                }
                int status;
                byte[] body;
                if (!"dev-key".equals(exchange.getRequestHeaders().getFirst("X-Service-Key"))) {
                    status = 401;
                    body = "{\"code\":\"INVALID_SERVICE_KEY\"}".getBytes(StandardCharsets.UTF_8);
                } else if (DATA.containsKey(path)) {
                    status = 200;
                    body = mapper.writeValueAsBytes(DATA.get(path));
                } else {
                    status = 500;
                    body = "{\"code\":\"BOOM\"}".getBytes(StandardCharsets.UTF_8);
                }
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, body.length);
                try (var output = exchange.getResponseBody()) {
                    output.write(body);
                }
            });
            server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
            server.start();
            return server;
        } catch (IOException error) {
            throw new ExceptionInInitializerError(error);
        }
    }
}
