package com.rikkei.jobs.profile.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.profile.dto.VerifiedSkillRecord;
import com.rikkei.jobs.profile.entity.ExternalSkillRecord;
import com.rikkei.jobs.profile.entity.MappingSource;
import com.rikkei.jobs.profile.repository.ExternalSkillRecordRepository;
import com.rikkei.jobs.profile.service.SkillProfileService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Tiêu chí nghiệm thu NT-12 trên API thật (H2, chế độ PostgreSQL). */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:nt12_admin;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.jwt.secret=test_secret_test_secret_test_secret_test_secret",
        "app.sync.nightly-cron=-",
        "app.sync.on-open-enabled=false",
        "logging.level.root=WARN"
})
@AutoConfigureMockMvc
class AdminApiTest {
    static final UUID CANDIDATE = UUID.fromString("12000000-0000-0000-0000-000000000001");
    static final UUID OTHER_CANDIDATE = UUID.fromString("12000000-0000-0000-0000-000000000002");
    static final UUID ADMIN = UUID.fromString("12000000-0000-0000-0000-000000000009");
    static final Instant RECORDED = Instant.parse("2026-10-20T01:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired SkillProfileService skillProfile;
    @Autowired ExternalSkillRecordRepository externalRecords;

    @BeforeEach
    void reset() {
        for (String table : List.of("change_logs", "data_reports", "manual_overrides", "skill_evidences",
                "skill_scores", "external_records", "skill_profile_states", "candidate_profiles")) {
            jdbc.update("DELETE FROM profile." + table);
        }
        jdbc.update("DELETE FROM profile.skills WHERE code LIKE 'NT12%'");
        jdbc.update("UPDATE profile.skills SET active = TRUE");
        jdbc.update("UPDATE profile.score_weights SET weight = CASE source WHEN 'LMS' THEN 30 WHEN 'EXAM' THEN 30 "
                + "WHEN 'INTERNSHIP' THEN 25 ELSE 15 END");
        jdbc.update("DELETE FROM profile.evaluation_templates WHERE name LIKE 'NT12%'");

        jdbc.execute("CREATE TABLE IF NOT EXISTS public.users (id UUID PRIMARY KEY, email VARCHAR(255) NOT NULL, "
                + "full_name VARCHAR(255) NOT NULL, role VARCHAR(20) NOT NULL, lms_user_id VARCHAR(100) UNIQUE, "
                + "status VARCHAR(10) NOT NULL DEFAULT 'ACTIVE')");
        jdbc.update("DELETE FROM public.users");
        jdbc.update("INSERT INTO public.users (id, email, full_name, role, lms_user_id) VALUES (?, ?, ?, 'CANDIDATE', ?)",
                CANDIDATE, "candidate1@rikkeijobs.test", "Nguyễn Văn An", "lms-sv-001");
        jdbc.update("INSERT INTO profile.candidate_profiles (user_id, full_name, email, program, class_code, "
                        + "course_name, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                CANDIDATE, "Nguyễn Văn An", "candidate1@rikkeijobs.test", "Java Backend", "JB-09", "Khoá 09",
                java.sql.Timestamp.from(RECORDED));
    }

    // ---------- Thêm kỹ năng mới → xuất hiện ở form JD và hồ sơ ----------

    @Test
    void newSkillAppearsInCatalogueUsedByJobFormAndProfile() throws Exception {
        JsonNode created = body(call(post("/admin/skills"), Role.ADMIN, ADMIN,
                Map.of("code", "nt12_kotlin", "name", "Kotlin", "group", "Backend", "reason", "Bổ sung theo JD mới"))
                .andExpect(status().isCreated()));
        assertThat(created.path("code").asText()).isEqualTo("NT12_KOTLIN");

        // GET /skills là danh mục mà form JD (company-service) và hồ sơ ứng viên cùng đọc.
        for (Role role : List.of(Role.HR, Role.CANDIDATE)) {
            JsonNode catalogue = body(call(get("/skills"), role, UUID.randomUUID(), null).andExpect(status().isOk()));
            JsonNode kotlin = find(catalogue, "code", "NT12_KOTLIN");
            assertThat(kotlin).as("kỹ năng mới trong danh mục của " + role).isNotNull();
            assertThat(kotlin.path("active").asBoolean()).isTrue();
        }
        // Ứng viên chọn được kỹ năng mới ở phần tự khai.
        call(put("/profiles/me/self-skills"), Role.CANDIDATE, CANDIDATE,
                List.of(Map.of("skillId", created.path("id").asText(), "selfScore", 60)))
                .andExpect(status().isOk());

        call(post("/admin/skills"), Role.ADMIN, ADMIN, Map.of("code", "NT12_KOTLIN", "name", "Trùng", "group", "X"))
                .andExpect(status().isConflict());
        assertThat(logs("SKILL")).extracting(l -> l.path("field").asText()).contains("code", "name", "group");
    }

    // ---------- Lưu trọng số tổng khác 100 → báo lỗi ----------

    @Test
    void weightsMustSumTo100() throws Exception {
        call(put("/admin/score-weights"), Role.ADMIN, ADMIN, weights(30, 30, 30, 15))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.weights").value("Tổng trọng số phải bằng 100 (hiện tại: 105)"));
        call(put("/admin/score-weights"), Role.ADMIN, ADMIN,
                Map.of("weights", List.of(Map.of("source", "LMS", "weight", 100))))
                .andExpect(status().isBadRequest());

        JsonNode saved = body(call(put("/admin/score-weights"), Role.ADMIN, ADMIN, weights(40, 20, 25, 15))
                .andExpect(status().isOk()));
        assertThat(saved.path("total").decimalValue()).isEqualByComparingTo("100");
        assertThat(saved.path("weights").get(0).path("source").asText()).isEqualTo("LMS");
        assertThat(saved.path("weights").get(0).path("weight").decimalValue()).isEqualByComparingTo("40");

        List<JsonNode> logs = logs("SCORE_WEIGHT");
        assertThat(logs).hasSize(2);
        JsonNode lms = logs.stream().filter(l -> l.path("entityId").asText().equals("LMS")).findFirst().orElseThrow();
        assertThat(lms.path("oldValue").asText()).isEqualTo("30");
        assertThat(lms.path("newValue").asText()).isEqualTo("40");

        // DG-03 đọc trọng số hiện hành qua API, không đọc thẳng bảng.
        call(get("/score-weights"), Role.MENTOR, UUID.randomUUID(), null)
                .andExpect(status().isOk()).andExpect(jsonPath("$.weights[0].weight").value(40));
        call(get("/score-weights"), Role.CANDIDATE, CANDIDATE, null).andExpect(status().isForbidden());
    }

    // ---------- Sửa bằng chứng không lý do → chặn; có lý do → change_logs giữ cũ/mới ----------

    @Test
    void evidenceEditRequiresReasonAndLogsOldAndNewValues() throws Exception {
        UUID evidenceId = directEvidence();

        call(put("/admin/evidences/" + evidenceId), Role.ADMIN, ADMIN, Map.of("score", 90))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
        call(put("/admin/evidences/" + evidenceId), Role.ADMIN, ADMIN, Map.of("score", 90, "reason", "  "))
                .andExpect(status().isBadRequest());
        assertThat(logs("EVIDENCE")).isEmpty();

        JsonNode updated = body(call(put("/admin/evidences/" + evidenceId), Role.ADMIN, ADMIN,
                Map.of("score", 90, "title", "Thi React (phúc khảo)", "reason", "Phúc khảo theo đơn 12/2026"))
                .andExpect(status().isOk()));
        assertThat(updated.path("score").decimalValue()).isEqualByComparingTo("90");

        List<JsonNode> logs = logs("EVIDENCE");
        assertThat(logs).hasSize(2).allSatisfy(l -> {
            assertThat(l.path("entityId").asText()).isEqualTo(evidenceId.toString());
            assertThat(l.path("reason").asText()).isEqualTo("Phúc khảo theo đơn 12/2026");
            assertThat(l.path("adminId").asText()).isEqualTo(ADMIN.toString());
        });
        JsonNode score = logs.stream().filter(l -> l.path("field").asText().equals("score")).findFirst().orElseThrow();
        assertThat(score.path("oldValue").asText()).isEqualTo("70");
        assertThat(score.path("newValue").asText()).isEqualTo("90");
        // Điểm kỹ năng được tính lại theo bằng chứng mới.
        assertThat(skillScore("REACTJS")).isEqualByComparingTo("90");
    }

    @Test
    void editingSyncedEvidenceSurvivesRecompute() throws Exception {
        UUID javaEvidence = externalEvidence("JAVA");

        call(put("/admin/evidences/" + javaEvidence), Role.ADMIN, ADMIN, Map.of("score", 95, "reason", "Sai điểm nhập"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(95))
                .andExpect(jsonPath("$.fromExternalRecord").value(true));

        // Đồng bộ hoặc tính lại sau đó không ghi đè giá trị đã sửa (dùng cơ chế sửa tay NT-05).
        skillProfile.recomputeSkills(CANDIDATE);
        assertThat(evidenceScore(javaEvidence)).isEqualByComparingTo("95");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM profile.manual_overrides", Integer.class)).isEqualTo(1);
        assertThat(logs("EVIDENCE")).singleElement().satisfies(l -> {
            assertThat(l.path("oldValue").asText()).isEqualTo("85");
            assertThat(l.path("newValue").asText()).isEqualTo("95");
        });
    }

    @Test
    void profileEditRequiresReasonAndLogsChangedFieldsOnly() throws Exception {
        Map<String, Object> request = new java.util.HashMap<>(Map.of(
                "fullName", "Nguyễn Văn An", "email", "an.nguyen@rikkeijobs.test", "phone", "0912345678"));
        call(put("/admin/profiles/" + CANDIDATE), Role.ADMIN, ADMIN, request).andExpect(status().isBadRequest());

        request.put("reason", "Ứng viên đổi email liên hệ");
        call(put("/admin/profiles/" + CANDIDATE), Role.ADMIN, ADMIN, request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("an.nguyen@rikkeijobs.test"));

        assertThat(logs("PROFILE")).extracting(l -> l.path("field").asText()).containsExactlyInAnyOrder("email", "phone");
    }

    // ---------- Ứng viên báo sai → admin thấy; xử lý xong ứng viên thấy kết quả ----------

    @Test
    void candidateReportIsSeenAndResolvedByAdmin() throws Exception {
        UUID evidenceId = directEvidence();
        JsonNode profile = body(call(get("/profiles/me/skills"), Role.CANDIDATE, CANDIDATE, null));
        assertThat(profile.findValues("id").stream().map(JsonNode::asText)).contains(evidenceId.toString());

        JsonNode report = body(call(post("/me/data-reports"), Role.CANDIDATE, CANDIDATE,
                Map.of("evidenceId", evidenceId, "content", "Điểm thi React của em là 90, không phải 70"))
                .andExpect(status().isCreated()));
        assertThat(report.path("status").asText()).isEqualTo("OPEN");
        call(post("/me/data-reports"), Role.CANDIDATE, CANDIDATE,
                Map.of("evidenceId", evidenceId, "content", "Báo lại")).andExpect(status().isConflict());
        // Bằng chứng của người khác coi như không tồn tại.
        call(post("/me/data-reports"), Role.CANDIDATE, OTHER_CANDIDATE,
                Map.of("evidenceId", evidenceId, "content", "x")).andExpect(status().isNotFound());

        JsonNode open = body(call(get("/admin/data-reports?status=OPEN"), Role.ADMIN, ADMIN, null)
                .andExpect(status().isOk()));
        assertThat(open.path("total").asInt()).isEqualTo(1);
        JsonNode item = open.path("items").get(0);
        assertThat(item.path("candidateName").asText()).isEqualTo("Nguyễn Văn An");
        assertThat(item.path("evidence").path("score").decimalValue()).isEqualByComparingTo("70");

        String id = report.path("id").asText();
        call(put("/admin/data-reports/" + id), Role.ADMIN, ADMIN, Map.of("status", "OPEN", "resolution", "x"))
                .andExpect(status().isBadRequest());
        call(put("/admin/data-reports/" + id), Role.ADMIN, ADMIN,
                Map.of("status", "RESOLVED", "resolution", "Đã sửa điểm thành 90 theo kết quả phúc khảo"))
                .andExpect(status().isOk());
        call(put("/admin/data-reports/" + id), Role.ADMIN, ADMIN,
                Map.of("status", "REJECTED", "resolution", "x")).andExpect(status().isConflict());

        JsonNode mine = body(call(get("/me/data-reports"), Role.CANDIDATE, CANDIDATE, null).andExpect(status().isOk()));
        assertThat(mine.get(0).path("status").asText()).isEqualTo("RESOLVED");
        assertThat(mine.get(0).path("resolution").asText()).isEqualTo("Đã sửa điểm thành 90 theo kết quả phúc khảo");
        assertThat(mine.get(0).path("resolvedAt").isNull()).isFalse();
        assertThat(logs("DATA_REPORT")).hasSize(1);
    }

    // ---------- Tắt kỹ năng đã có bằng chứng → dữ liệu cũ còn nguyên ----------

    @Test
    void deactivatingUsedSkillKeepsOldData() throws Exception {
        directEvidence();
        String reactId = jdbc.queryForObject("SELECT id FROM profile.skills WHERE code = 'REACTJS'", String.class);
        BigDecimal before = skillScore("REACTJS");

        call(delete("/admin/skills/" + reactId + "?reason=Gộp vào ReactJS/Next.js"), Role.ADMIN, ADMIN, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deleted").value(false))
                .andExpect(jsonPath("$.deactivated").value(true));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM profile.skills WHERE code = 'REACTJS'", Integer.class))
                .isEqualTo(1);
        assertThat(skillScore("REACTJS")).isEqualByComparingTo(before);
        JsonNode profile = body(call(get("/profiles/me/skills"), Role.CANDIDATE, CANDIDATE, null));
        JsonNode react = find(profile, "code", "REACTJS");
        assertThat(react).as("hồ sơ vẫn hiện kỹ năng đã tắt cùng bằng chứng cũ").isNotNull();
        JsonNode catalogue = body(call(get("/skills"), Role.HR, UUID.randomUUID(), null));
        assertThat(find(catalogue, "code", "REACTJS").path("active").asBoolean()).isFalse();
        // Không chọn mới được kỹ năng đã tắt.
        call(put("/profiles/me/self-skills"), Role.CANDIDATE, CANDIDATE,
                List.of(Map.of("skillId", reactId, "selfScore", 50))).andExpect(status().isBadRequest());

        // Kỹ năng chưa dùng thì xoá hẳn.
        JsonNode unused = body(call(post("/admin/skills"), Role.ADMIN, ADMIN,
                Map.of("code", "NT12_UNUSED", "name", "Chưa dùng", "group", "Khác")));
        call(delete("/admin/skills/" + unused.path("id").asText()), Role.ADMIN, ADMIN, null)
                .andExpect(jsonPath("$.deleted").value(true));
    }

    // ---------- Mẫu phiếu kèm tiêu chí ----------

    @Test
    void evaluationTemplateCrudReplacesCriteria() throws Exception {
        Map<String, Object> request = Map.of("name", "NT12 Đánh giá giữa kỳ", "criteria", List.of(
                Map.of("name", "Chuyên môn", "weight", 60, "maxScore", 10),
                Map.of("name", "Thái độ", "weight", 40, "maxScore", 10)));
        JsonNode created = body(call(post("/admin/evaluation-templates"), Role.ADMIN, ADMIN, request)
                .andExpect(status().isCreated()));
        String id = created.path("id").asText();
        assertThat(created.path("criteria")).hasSize(2);

        call(put("/admin/evaluation-templates/" + id), Role.ADMIN, ADMIN, Map.of(
                "name", "NT12 Đánh giá giữa kỳ", "active", false, "reason", "Tạm ngưng",
                "criteria", List.of(Map.of("name", "Tổng hợp", "weight", 100, "maxScore", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criteria.length()").value(1))
                .andExpect(jsonPath("$.criteria[0].maxScore").value(5));
        call(post("/admin/evaluation-templates"), Role.ADMIN, ADMIN, Map.of("name", "NT12 thiếu tiêu chí",
                "criteria", List.of())).andExpect(status().isBadRequest());

        // Mẫu đã tắt không hiện cho mentor (DG-01).
        JsonNode active = body(call(get("/evaluation-templates"), Role.MENTOR, UUID.randomUUID(), null));
        assertThat(find(active, "id", id)).isNull();

        call(delete("/admin/evaluation-templates/" + id), Role.ADMIN, ADMIN, null).andExpect(status().isNoContent());
        call(get("/admin/evaluation-templates/" + id), Role.ADMIN, ADMIN, null).andExpect(status().isNotFound());
        assertThat(logs("EVALUATION_TEMPLATE")).extracting(l -> l.path("field").asText())
                .contains("name", "criteria", "active", "deleted");
    }

    @Test
    void changeLogsFilterByEntityAndDate() throws Exception {
        call(put("/admin/score-weights"), Role.ADMIN, ADMIN, weights(40, 20, 25, 15)).andExpect(status().isOk());
        call(post("/admin/skills"), Role.ADMIN, ADMIN, Map.of("code", "NT12_GO", "name", "Go", "group", "Backend"));

        assertThat(body(call(get("/admin/change-logs"), Role.ADMIN, ADMIN, null)).path("total").asInt()).isEqualTo(5);
        assertThat(body(call(get("/admin/change-logs?entity=SKILL"), Role.ADMIN, ADMIN, null)).path("total").asInt())
                .isEqualTo(3);
        assertThat(body(call(get("/admin/change-logs?from=2000-01-01&to=2000-01-02"), Role.ADMIN, ADMIN, null))
                .path("total").asInt()).isZero();
        call(get("/admin/change-logs?from=2026-12-02&to=2026-12-01"), Role.ADMIN, ADMIN, null)
                .andExpect(status().isBadRequest());
    }

    // ---------- Token không phải ADMIN gọi /admin/* → 403 ----------

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"CANDIDATE", "HR", "MENTOR", "STAFF"})
    void nonAdminGets403OnAdminApis(Role role) throws Exception {
        UUID user = UUID.randomUUID();
        UUID any = UUID.randomUUID();
        for (MockHttpServletRequestBuilder request : List.of(get("/admin/skills"), post("/admin/skills"),
                delete("/admin/skills/" + any), get("/admin/score-weights"), put("/admin/score-weights"),
                get("/admin/evaluation-templates"), put("/admin/profiles/" + any), put("/admin/evidences/" + any),
                get("/admin/data-reports"), put("/admin/data-reports/" + any), get("/admin/change-logs"))) {
            call(request, role, user, Map.of()).andExpect(status().isForbidden());
        }
        mvc.perform(get("/admin/skills")).andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    /** Bằng chứng nhập trực tiếp (không từ bản ghi nguồn): Thi React 70 điểm → kỹ năng REACTJS. */
    private UUID directEvidence() {
        skillProfile.importVerifiedRecords(CANDIDATE, List.of(new VerifiedSkillRecord(MappingSource.EXAM, "REACT01",
                "exam-react-01", "Thi React", BigDecimal.valueOf(70), RECORDED)));
        return evidenceId("REACTJS");
    }

    /** Bản ghi LMS JAVA01 8,5/10 như NT-05 đồng bộ về, ánh xạ sang JAVA và SQL. */
    private UUID externalEvidence(String skillCode) {
        ExternalSkillRecord record = new ExternalSkillRecord();
        record.setUserId(CANDIDATE);
        record.setSource(MappingSource.LMS);
        record.setSourceCode("JAVA01");
        record.setSourceRef("lms-sv-001:JAVA01");
        record.setTitle("Lập trình Java");
        record.setScore(new BigDecimal("8.5"));
        record.setScoreMax(BigDecimal.TEN);
        record.setRecordedAt(RECORDED);
        record.setVerified(true);
        externalRecords.saveAndFlush(record);
        skillProfile.recomputeSkills(CANDIDATE);
        return evidenceId(skillCode);
    }

    private UUID evidenceId(String skillCode) {
        return jdbc.queryForObject("SELECT e.id FROM profile.skill_evidences e JOIN profile.skills s "
                + "ON s.id = e.skill_id WHERE e.user_id = ? AND s.code = ?", UUID.class, CANDIDATE, skillCode);
    }

    private BigDecimal evidenceScore(UUID id) {
        return jdbc.queryForObject("SELECT score FROM profile.skill_evidences WHERE id = ?", BigDecimal.class, id);
    }

    private BigDecimal skillScore(String code) {
        return jdbc.queryForObject("SELECT sc.verified_score FROM profile.skill_scores sc JOIN profile.skills s "
                + "ON s.id = sc.skill_id WHERE sc.user_id = ? AND s.code = ?", BigDecimal.class, CANDIDATE, code);
    }

    private static Map<String, Object> weights(int lms, int exam, int internship, int mentor) {
        return Map.of("reason", "Điều chỉnh theo quy chế 2026", "weights", List.of(
                Map.of("source", "LMS", "weight", lms), Map.of("source", "EXAM", "weight", exam),
                Map.of("source", "INTERNSHIP", "weight", internship), Map.of("source", "MENTOR", "weight", mentor)));
    }

    private List<JsonNode> logs(String entity) throws Exception {
        JsonNode page = body(call(get("/admin/change-logs?size=100&entity=" + entity), Role.ADMIN, ADMIN, null)
                .andExpect(status().isOk()));
        return java.util.stream.StreamSupport.stream(page.path("items").spliterator(), false).toList();
    }

    /** Tìm phần tử (kể cả lồng trong "skill") có field = value. */
    private static JsonNode find(JsonNode array, String field, String value) {
        for (JsonNode item : array) {
            JsonNode target = item.has("skill") ? item.path("skill") : item;
            if (value.equals(target.path(field).asText())) {
                return target;
            }
        }
        return null;
    }

    private ResultActions call(MockHttpServletRequestBuilder request, Role role, UUID user, Object body)
            throws Exception {
        request.header("Authorization", "Bearer " + jwt.generateJwtToken(user, role, null));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        return mvc.perform(request);
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }
}
