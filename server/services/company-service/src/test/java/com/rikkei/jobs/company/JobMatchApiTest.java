package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.client.CatalogSkill;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.entity.MatchScore;
import com.rikkei.jobs.company.entity.MatchScoreId;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class JobMatchApiTest extends CompanyApiTestSupport {

    private static final UUID JAVA = skill(1);
    private static final UUID SPRING_BOOT = skill(2);
    private static final UUID SQL = skill(3);
    private static final UUID GIT = skill(4);
    private static final UUID DOCKER = skill(11);
    private static final UUID COMMUNICATION = skill(18);

    private static final UUID C1 = UUID.fromString("00000000-0000-0000-0002-000000000001");
    private static final UUID C3 = UUID.fromString("00000000-0000-0000-0002-000000000003");

    @Autowired private ObjectMapper json;

    private Company company;

    private static UUID skill(int n) {
        return UUID.fromString(String.format("00000000-0000-0000-0001-%012d", n));
    }

    @BeforeEach
    void setUp() {
        company = saveCompany("1111111111", CompanyStatus.ACTIVE);
        linkHr(HR1, company.getId());
        stubCatalog(List.of(
                new CatalogSkill(JAVA, "JAVA", "Java"),
                new CatalogSkill(SPRING_BOOT, "SPRING_BOOT", "Spring Boot"),
                new CatalogSkill(SQL, "SQL", "SQL"),
                new CatalogSkill(GIT, "GIT", "Git"),
                new CatalogSkill(DOCKER, "DOCKER", "Docker"),
                new CatalogSkill(COMMUNICATION, "COMMUNICATION", "Giao tiếp")));
        when(skillCatalog.skillIds(anyString())).thenReturn(
                Set.of(JAVA, SPRING_BOOT, SQL, GIT, DOCKER, COMMUNICATION));
        when(profileSkills.verifiedScores(eq(C1), anyString())).thenReturn(c1Scores(new BigDecimal("81.153846")));
        when(profileSkills.verifiedScores(eq(C3), anyString())).thenReturn(Map.of(
                JAVA, new BigDecimal("49.117647"), SQL, new BigDecimal("58.846154")));
    }

    private void stubCatalog(List<CatalogSkill> skills) {
        when(skillCatalog.skills(anyString())).thenReturn(skills);
    }

    private static Map<UUID, BigDecimal> c1Scores(BigDecimal sql) {
        return Map.of(
                JAVA, new BigDecimal("82.058824"),
                SPRING_BOOT, new BigDecimal("80.454545"),
                SQL, sql,
                GIT, new BigDecimal("90"));
    }

    /** JD6: bắt buộc Java 80, Spring Boot 70, SQL 90; ưu tiên Docker 50, Giao tiếp 60, Git 60. */
    private Job jd6(UUID companyId, JobStatus status) {
        Job job = saveJob(companyId, status);
        jobSkills.save(new JobSkill(job.getId(), JAVA, true, 80));
        jobSkills.save(new JobSkill(job.getId(), SPRING_BOOT, true, 70));
        jobSkills.save(new JobSkill(job.getId(), SQL, true, 90));
        jobSkills.save(new JobSkill(job.getId(), DOCKER, false, 50));
        jobSkills.save(new JobSkill(job.getId(), COMMUNICATION, false, 60));
        jobSkills.save(new JobSkill(job.getId(), GIT, false, 60));
        return job;
    }

    /** JD7: chỉ bắt buộc Java 60, SQL 60, Git 60. */
    private Job jd7(UUID companyId, JobStatus status) {
        Job job = saveJob(companyId, status);
        jobSkills.save(new JobSkill(job.getId(), JAVA, true, 60));
        jobSkills.save(new JobSkill(job.getId(), SQL, true, 60));
        jobSkills.save(new JobSkill(job.getId(), GIT, true, 60));
        return job;
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private ResultActions meAs(UUID userId, Role role, UUID jobId) throws Exception {
        return mvc.perform(get("/jobs/" + jobId + "/match/me")
                .header("Authorization", bearer(token(userId, role))));
    }

    private ResultActions viewAs(UUID viewerId, Role role, UUID jobId, UUID userId) throws Exception {
        return mvc.perform(get("/jobs/" + jobId + "/match/" + userId)
                .header("Authorization", bearer(token(viewerId, role))));
    }

    private ResultActions staffView(UUID jobId, UUID userId) throws Exception {
        return viewAs(UUID.randomUUID(), Role.STAFF, jobId, userId);
    }

    private ResultActions putSkills(UUID jobId, List<Map<String, Object>> skills) throws Exception {
        return mvc.perform(put("/jobs/" + jobId + "/skills")
                .header("Authorization", bearer(token(HR1, Role.HR)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(skills)));
    }

    private static Map<String, Object> skillBody(UUID id, boolean required, int minScore) {
        return Map.of("skillId", id.toString(), "required", required, "minScore", minScore);
    }

    @Test
    void candidateMeOnApprovedJob() throws Exception {
        Job job = jd6(company.getId(), JobStatus.APPROVED);

        meAs(C1, Role.CANDIDATE, job.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(78))
                .andExpect(jsonPath("$.belowMinimum").value(false))
                .andExpect(jsonPath("$.met[0].skill").value("Java"))
                .andExpect(jsonPath("$.missing[2].skill").value("Giao tiếp"))
                .andExpect(jsonPath("$.missing[2].candidateScore").value(0));
    }

    @Test
    void candidateMeOnUnpublishedJobIs404() throws Exception {
        for (JobStatus status : List.of(JobStatus.DRAFT, JobStatus.PENDING, JobStatus.REJECTED, JobStatus.CLOSED)) {
            Job job = jd6(company.getId(), status);
            meAs(C1, Role.CANDIDATE, job.getId())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
        verifyNoInteractions(profileSkills);
    }

    @Test
    void hrOwnJobAppliedCandidate() throws Exception {
        Job job = jd6(company.getId(), JobStatus.APPROVED);
        when(applications.hasApplied(eq(job.getId()), eq(C1), anyString())).thenReturn(true);
        String auth = bearer(token(HR1, Role.HR));

        mvc.perform(get("/jobs/" + job.getId() + "/match/" + C1).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(78));

        verify(applications).hasApplied(job.getId(), C1, auth);
    }

    @Test
    void hrNotAppliedIs404() throws Exception {
        Job job = jd6(company.getId(), JobStatus.APPROVED);
        when(applications.hasApplied(eq(job.getId()), eq(C1), anyString())).thenReturn(false);

        viewAs(HR1, Role.HR, job.getId(), C1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy JD"));

        verifyNoInteractions(profileSkills);
    }

    @Test
    void hrOtherCompanyJobIs404() throws Exception {
        Company other = saveCompany("2222222222", CompanyStatus.ACTIVE);
        Job job = jd6(other.getId(), JobStatus.APPROVED);

        viewAs(HR1, Role.HR, job.getId(), C1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy JD"));

        verifyNoInteractions(applications);
    }

    @Test
    void staffSeesAnyJobWithoutRecruitment() throws Exception {
        Job job = jd7(company.getId(), JobStatus.DRAFT);

        staffView(job.getId(), C3)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(60))
                .andExpect(jsonPath("$.belowMinimum").value(true))
                .andExpect(jsonPath("$.met", hasSize(0)))
                .andExpect(jsonPath("$.missing[0].skill").value("Git"))
                .andExpect(jsonPath("$.missing[1].skill").value("Java"))
                .andExpect(jsonPath("$.missing[2].skill").value("SQL"));

        verifyNoInteractions(applications);
    }

    @Test
    void wrongRolesAre403() throws Exception {
        Job job = jd6(company.getId(), JobStatus.APPROVED);

        viewAs(C1, Role.CANDIDATE, job.getId(), C1).andExpect(status().isForbidden());
        meAs(UUID.randomUUID(), Role.MENTOR, job.getId()).andExpect(status().isForbidden());
        viewAs(UUID.randomUUID(), Role.ADMIN, job.getId(), C1).andExpect(status().isForbidden());
    }

    @Test
    void unknownJobIs404() throws Exception {
        staffView(UUID.randomUUID(), C1)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void responseMatchesUt02Contract() throws Exception {
        Job job = jd6(company.getId(), JobStatus.APPROVED);

        String body = meAs(C1, Role.CANDIDATE, job.getId())
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        JsonNode root = json.readTree(body);
        assertThat(fieldNames(root)).containsExactlyInAnyOrder("score", "belowMinimum", "met", "missing");
        List<JsonNode> items = new ArrayList<>();
        root.get("met").forEach(items::add);
        root.get("missing").forEach(items::add);
        assertThat(items).hasSize(6);
        for (JsonNode item : items) {
            assertThat(fieldNames(item)).containsExactlyInAnyOrder("skill", "required", "minScore", "candidateScore");
            assertThat(item.get("skill").asText()).isNotBlank();
            assertThat(item.get("candidateScore").isNumber()).isTrue();
        }
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new HashSet<>();
        Iterator<String> it = node.fieldNames();
        it.forEachRemaining(names::add);
        return names;
    }

    @Test
    void minScoreChangeGivesNewScore() throws Exception {
        Job job = jd6(company.getId(), JobStatus.DRAFT);

        staffView(job.getId(), C1).andExpect(jsonPath("$.score").value(78));

        putSkills(job.getId(), List.of(
                skillBody(JAVA, true, 80), skillBody(SPRING_BOOT, true, 70), skillBody(SQL, true, 80),
                skillBody(DOCKER, false, 50), skillBody(COMMUNICATION, false, 60), skillBody(GIT, false, 60)))
                .andExpect(status().isOk());

        assertThat(matchScores.findById(new MatchScoreId(job.getId(), C1)).orElseThrow().isStale()).isTrue();
        staffView(job.getId(), C1).andExpect(jsonPath("$.score").value(80));
    }

    @Test
    void verifiedScoreChangeGivesNewScore() throws Exception {
        Job job = jd6(company.getId(), JobStatus.APPROVED);

        meAs(C1, Role.CANDIDATE, job.getId()).andExpect(jsonPath("$.score").value(78));

        when(profileSkills.verifiedScores(eq(C1), anyString())).thenReturn(c1Scores(new BigDecimal("95")));
        meAs(C1, Role.CANDIDATE, job.getId()).andExpect(jsonPath("$.score").value(80));

        assertThat(matchScores.findById(new MatchScoreId(job.getId(), C1)).orElseThrow().getScore()).isEqualTo(80);
    }

    @Test
    void cachedResultReusedWhenInputsUnchanged() throws Exception {
        Job job = jd6(company.getId(), JobStatus.APPROVED);

        meAs(C1, Role.CANDIDATE, job.getId()).andExpect(jsonPath("$.score").value(78));
        meAs(C1, Role.CANDIDATE, job.getId()).andExpect(jsonPath("$.score").value(78));

        verify(skillCatalog, times(1)).skills(anyString());
    }

    @Test
    void cacheHitIgnoresDecimalScale() throws Exception {
        Job job = jd6(company.getId(), JobStatus.APPROVED);

        meAs(C1, Role.CANDIDATE, job.getId()).andExpect(status().isOk());
        when(profileSkills.verifiedScores(eq(C1), anyString())).thenReturn(c1Scores(new BigDecimal("81.1538460")));
        meAs(C1, Role.CANDIDATE, job.getId()).andExpect(jsonPath("$.score").value(78));

        verify(skillCatalog, times(1)).skills(anyString());
    }

    @Test
    void jobSkillsClearedGivesZero() throws Exception {
        Job job = jd6(company.getId(), JobStatus.DRAFT);

        staffView(job.getId(), C1).andExpect(jsonPath("$.score").value(78));
        putSkills(job.getId(), List.of()).andExpect(status().isOk());

        staffView(job.getId(), C1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(0))
                .andExpect(jsonPath("$.met", hasSize(0)))
                .andExpect(jsonPath("$.missing", hasSize(0)));
    }

    @Test
    void unreadableDetailIsRecomputed() throws Exception {
        Job job = jd6(company.getId(), JobStatus.APPROVED);
        matchScores.save(new MatchScore(job.getId(), C1, 5, "not json", false, Instant.now()));

        meAs(C1, Role.CANDIDATE, job.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(78));
    }

    @Test
    void skillMissingFromCatalogFallsBackToId() throws Exception {
        stubCatalog(List.of(
                new CatalogSkill(JAVA, "JAVA", "Java"),
                new CatalogSkill(SPRING_BOOT, "SPRING_BOOT", "Spring Boot"),
                new CatalogSkill(SQL, "SQL", "SQL"),
                new CatalogSkill(GIT, "GIT", "Git"),
                new CatalogSkill(COMMUNICATION, "COMMUNICATION", "Giao tiếp")));
        Job job = jd6(company.getId(), JobStatus.APPROVED);

        meAs(C1, Role.CANDIDATE, job.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missing[?(@.skill == '" + DOCKER + "')]", hasSize(1)));
    }

    @Test
    void blankCatalogNameFallsBackToId() throws Exception {
        stubCatalog(List.of(
                new CatalogSkill(JAVA, "JAVA", "Java"),
                new CatalogSkill(SPRING_BOOT, "SPRING_BOOT", "Spring Boot"),
                new CatalogSkill(SQL, "SQL", "SQL"),
                new CatalogSkill(GIT, "GIT", "Git"),
                new CatalogSkill(COMMUNICATION, "COMMUNICATION", "Giao tiếp"),
                new CatalogSkill(DOCKER, "", "")));
        Job job = jd6(company.getId(), JobStatus.APPROVED);

        meAs(C1, Role.CANDIDATE, job.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.missing[?(@.skill == '" + DOCKER + "')]", hasSize(1)));
    }

    @Test
    void dependencyDownIs503() throws Exception {
        Job job = jd6(company.getId(), JobStatus.APPROVED);
        CompanyException down = new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE, "x");

        when(profileSkills.verifiedScores(any(), anyString())).thenThrow(down);
        meAs(C1, Role.CANDIDATE, job.getId())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_UNAVAILABLE"));

        when(applications.hasApplied(any(), any(), anyString())).thenThrow(down);
        viewAs(HR1, Role.HR, job.getId(), C1)
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_UNAVAILABLE"));

        doReturn(c1Scores(new BigDecimal("81.153846"))).when(profileSkills).verifiedScores(eq(C1), anyString());
        when(skillCatalog.skills(anyString())).thenThrow(down);
        meAs(C1, Role.CANDIDATE, job.getId())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_UNAVAILABLE"));
    }

    @Test
    void putWithoutSkillsKeepsCacheFresh() throws Exception {
        Job job = jd6(company.getId(), JobStatus.DRAFT);
        staffView(job.getId(), C1).andExpect(status().isOk());
        MatchScoreId id = new MatchScoreId(job.getId(), C1);
        String auth = bearer(token(HR1, Role.HR));

        mvc.perform(put("/jobs/" + job.getId()).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Tiêu đề mới"))))
                .andExpect(status().isOk());
        assertThat(matchScores.findById(id).orElseThrow().isStale()).isFalse();

        mvc.perform(put("/jobs/" + job.getId()).header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Tiêu đề mới",
                                "skills", List.of(skillBody(JAVA, true, 60))))))
                .andExpect(status().isOk());
        assertThat(matchScores.findById(id).orElseThrow().isStale()).isTrue();
    }
}
