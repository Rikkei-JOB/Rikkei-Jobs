package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class JobApiTest extends CompanyApiTestSupport {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Id cố định của danh mục kỹ năng (V2 của profile-service). */
    private static final UUID JAVA = UUID.fromString("00000000-0000-0000-0001-000000000001");
    private static final UUID SQL = UUID.fromString("00000000-0000-0000-0001-000000000003");
    private static final UUID DOCKER = UUID.fromString("00000000-0000-0000-0001-000000000011");

    private Company a;
    private Company b;

    @BeforeEach
    void setUp() {
        a = saveCompany("1111111111", CompanyStatus.ACTIVE);
        b = saveCompany("2222222222", CompanyStatus.ACTIVE);
        linkHr(HR1, a.getId());
        linkHr(HR2, b.getId());
        when(skillCatalog.skillIds(any())).thenReturn(Set.of(JAVA, SQL, DOCKER));
    }

    // ---- tiện ích ----

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private String bearer(UUID userId, Role role) {
        return "Bearer " + token(userId, role);
    }

    private String hr1() {
        return bearer(HR1, Role.HR);
    }

    private String hr2() {
        return bearer(HR2, Role.HR);
    }

    /** Map cho phép giá trị null, theo cặp khoá/giá trị. */
    private static Map<String, Object> body(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static Map<String, Object> skill(UUID id, Boolean required, Integer minScore) {
        return body("skillId", id == null ? null : id.toString(), "required", required, "minScore", minScore);
    }

    /** Toàn bộ thông tin đủ điều kiện gửi duyệt (không kèm skills). */
    private static Map<String, Object> fullBody() {
        return body("title", "Lập trình viên Java", "position", "Backend", "level", "JUNIOR", "type", "FULLTIME",
                "quantity", 2, "city", "Hà Nội", "workMode", "ONSITE", "salaryMin", 10_000_000L,
                "salaryMax", 20_000_000L, "description", "Phát triển API", "deadline", today().plusDays(10).toString());
    }

    private ResultActions postJob(String auth, Object body) throws Exception {
        return mvc.perform(post("/jobs").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    private ResultActions putJob(String auth, UUID id, Object body) throws Exception {
        return mvc.perform(put("/jobs/" + id).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    private ResultActions putSkills(String auth, UUID id, Object body) throws Exception {
        return mvc.perform(put("/jobs/" + id + "/skills").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    private ResultActions getJob(String auth, UUID id) throws Exception {
        return mvc.perform(get("/companies/me/jobs/" + id).header("Authorization", auth));
    }

    private ResultActions listMine(String auth, String query) throws Exception {
        return mvc.perform(get("/companies/me/jobs" + query).header("Authorization", auth));
    }

    /** JD đầy đủ, có một kỹ năng bắt buộc JAVA 60: qua được kiểm tra gửi duyệt. */
    private Job eligibleJob(UUID companyId, JobStatus status) {
        Job job = new Job();
        job.setCompanyId(companyId);
        job.setTitle("Lập trình viên Java");
        job.setPosition("Backend");
        job.setLevel(JobLevel.JUNIOR);
        job.setType(JobType.FULLTIME);
        job.setQuantity(2);
        job.setCity("Hà Nội");
        job.setWorkMode(WorkMode.ONSITE);
        job.setSalaryMin(10_000_000L);
        job.setSalaryMax(20_000_000L);
        job.setDescription("Phát triển API");
        job.setDeadline(today().plusDays(10));
        job.setStatus(status);
        job.setCreatedBy(UUID.randomUUID());
        if (status == JobStatus.APPROVED) {
            job.setReviewedBy(UUID.randomUUID());
            job.setReviewedAt(Instant.parse("2026-11-10T03:00:00Z"));
        }
        Job saved = jobs.save(job);
        jobSkills.save(new JobSkill(saved.getId(), JAVA, true, 60));
        return saved;
    }

    private Job reload(Job job) {
        return jobs.findById(job.getId()).orElseThrow();
    }

    // ---- tạo ----

    @Test
    void createWithTitleOnlyIsDraftOfOwnCompany() throws Exception {
        String id = JSON.readTree(postJob(hr1(), body("title", "Lập trình viên Java",
                        "companyId", b.getId().toString(), "status", "APPROVED"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.companyId").value(a.getId().toString()))
                .andExpect(jsonPath("$.title").value("Lập trình viên Java"))
                .andExpect(jsonPath("$.skills", hasSize(0)))
                .andExpect(jsonPath("$.rejectReason").value(nullValue()))
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        Job saved = jobs.findById(UUID.fromString(id)).orElseThrow();
        assertThat(saved.getCompanyId()).isEqualTo(a.getId());
        assertThat(saved.getCreatedBy()).isEqualTo(HR1);
        assertThat(saved.getStatus()).isEqualTo(JobStatus.DRAFT);
        verify(skillCatalog, never()).skillIds(any());
    }

    @Test
    void createWithSkillsPersistsSkills() throws Exception {
        String id = JSON.readTree(postJob(hr1(), body("title", "Lập trình viên Java",
                        "skills", List.of(skill(SQL, false, 40), skill(JAVA, true, 70))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.skills", hasSize(2)))
                .andExpect(jsonPath("$.skills[0].skillId").value(JAVA.toString()))
                .andExpect(jsonPath("$.skills[0].required").value(true))
                .andExpect(jsonPath("$.skills[1].skillId").value(SQL.toString()))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        List<JobSkill> rows = jobSkills.findByJobId(UUID.fromString(id));
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(JobSkill::getSkillId).containsExactlyInAnyOrder(JAVA, SQL);
    }

    @Test
    void createAndUpdateInternshipType() throws Exception {
        String id = JSON.readTree(postJob(hr1(), body("title", "Thực tập sinh", "type", "INTERNSHIP",
                        "level", "INTERN"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("INTERNSHIP"))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        UUID jobId = UUID.fromString(id);
        getJob(hr1(), jobId).andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INTERNSHIP"))
                .andExpect(jsonPath("$.level").value("INTERN"));

        putJob(hr1(), jobId, body("title", "Thực tập sinh", "type", "FULLTIME")).andExpect(status().isOk());
        putJob(hr1(), jobId, body("title", "Thực tập sinh", "type", "INTERNSHIP")).andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INTERNSHIP"));
        getJob(hr1(), jobId).andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("INTERNSHIP"));
        assertThat(jobs.findById(jobId).orElseThrow().getType()).isEqualTo(JobType.INTERNSHIP);
    }

    @Test
    void pastDeadlineRejectedOnSave() throws Exception {
        postJob(hr1(), body("title", "JD", "deadline", today().minusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.deadline").exists());
        assertThat(jobs.count()).isZero();

        postJob(hr1(), body("title", "JD", "deadline", today().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deadline").value(today().toString()));

        Job job = saveJob(a.getId(), JobStatus.DRAFT);
        putJob(hr1(), job.getId(), body("title", "JD", "deadline", today().minusDays(1).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.deadline").exists());
    }

    @Test
    void salaryMinGreaterThanMaxRejected() throws Exception {
        postJob(hr1(), body("title", "JD", "salaryMin", 20_000_000L, "salaryMax", 10_000_000L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.salaryMax").exists());
        assertThat(jobs.count()).isZero();
    }

    @Test
    void negativeSalaryRejectedLargeSalaryStored() throws Exception {
        postJob(hr1(), body("title", "JD", "salaryMin", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.salaryMin").exists());
        postJob(hr1(), body("title", "JD", "salaryMax", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.salaryMax").exists());
        assertThat(jobs.count()).isZero();

        String id = JSON.readTree(postJob(hr1(), body("title", "JD", "salaryMin", 0L, "salaryMax", 1_000_000_000_000L))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        Job saved = jobs.findById(UUID.fromString(id)).orElseThrow();
        assertThat(saved.getSalaryMin()).isZero();
        assertThat(saved.getSalaryMax()).isEqualTo(1_000_000_000_000L);
    }

    @Test
    void blankStringsBecomeNullAndBlankTitleRejected() throws Exception {
        postJob(hr1(), body("title", "   ", "city", "Hà Nội"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
        assertThat(jobs.count()).isZero();

        String id = JSON.readTree(postJob(hr1(), body("title", "  Java  ", "city", "  ", "position", "",
                        "address", " ", "description", "   ", "requirements", "\t", "benefits", "  Lương tháng 13  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Java"))
                .andExpect(jsonPath("$.city").value(nullValue()))
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        Job saved = jobs.findById(UUID.fromString(id)).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("Java");
        assertThat(saved.getCity()).isNull();
        assertThat(saved.getPosition()).isNull();
        assertThat(saved.getAddress()).isNull();
        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getRequirements()).isNull();
        assertThat(saved.getBenefits()).isEqualTo("Lương tháng 13");

        putJob(hr1(), saved.getId(), body("title", "  ")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
        assertThat(reload(saved).getTitle()).isEqualTo("Java");
    }

    // ---- sửa ----

    @Test
    void putReplacesGeneralInfoAndSkillsInOneCall() throws Exception {
        Job job = saveJob(a.getId(), JobStatus.DRAFT);
        Map<String, Object> req = body("title", "Tiêu đề mới", "city", "Hà Nội", "quantity", 3,
                "skills", List.of(skill(SQL, false, 90), skill(JAVA, true, 40)));
        putJob(hr1(), job.getId(), req).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Tiêu đề mới"))
                .andExpect(jsonPath("$.city").value("Hà Nội"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.skills", hasSize(2)))
                .andExpect(jsonPath("$.skills[0].skillId").value(JAVA.toString()))
                .andExpect(jsonPath("$.skills[0].required").value(true))
                .andExpect(jsonPath("$.skills[0].minScore").value(40))
                .andExpect(jsonPath("$.skills[1].skillId").value(SQL.toString()))
                .andExpect(jsonPath("$.skills[1].required").value(false))
                .andExpect(jsonPath("$.skills[1].minScore").value(90));

        // Không gửi skills: kỹ năng giữ nguyên; thông tin chung bị thay toàn bộ (city về null).
        putJob(hr1(), job.getId(), body("title", "Lần hai")).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lần hai"))
                .andExpect(jsonPath("$.city").value(nullValue()))
                .andExpect(jsonPath("$.quantity").value(nullValue()))
                .andExpect(jsonPath("$.skills", hasSize(2)))
                .andExpect(jsonPath("$.skills[0].skillId").value(JAVA.toString()));
        verify(skillCatalog, times(1)).skillIds(any());
        assertThat(jobSkills.findByJobId(job.getId())).hasSize(2);

        // Chỉ đổi kỹ năng qua PUT /skills: version vẫn tăng; sắp bắt buộc trước rồi minScore giảm dần.
        long before = reload(job).getVersion();
        putSkills(hr1(), job.getId(), List.of(skill(SQL, false, 50), skill(DOCKER, false, 80), skill(JAVA, true, 30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lần hai"))
                .andExpect(jsonPath("$.skills[0].skillId").value(JAVA.toString()))
                .andExpect(jsonPath("$.skills[1].skillId").value(DOCKER.toString()))
                .andExpect(jsonPath("$.skills[2].skillId").value(SQL.toString()));
        assertThat(reload(job).getVersion()).isGreaterThan(before);

        // Mảng rỗng hợp lệ: xoá hết kỹ năng.
        putJob(hr1(), job.getId(), body("title", "Lần ba", "skills", List.of())).andExpect(status().isOk())
                .andExpect(jsonPath("$.skills", hasSize(0)));
        assertThat(jobSkills.findByJobId(job.getId())).isEmpty();
    }

    @Test
    void unknownSkillRejectedAndNothingWritten() throws Exception {
        Job job = saveJob(a.getId(), JobStatus.DRAFT);
        jobSkills.save(new JobSkill(job.getId(), JAVA, true, 60));

        putJob(hr1(), job.getId(), body("title", "Đổi tên", "skills", List.of(skill(UUID.randomUUID(), true, 50))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.skills").value("Kỹ năng không có trong danh mục"));
        putSkills(hr1(), job.getId(), List.of(skill(SQL, true, 50), skill(UUID.randomUUID(), false, 50)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.skills").value("Kỹ năng không có trong danh mục"));
        postJob(hr1(), body("title", "Mới", "skills", List.of(skill(UUID.randomUUID(), true, 50))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.skills").value("Kỹ năng không có trong danh mục"));

        assertThat(reload(job).getTitle()).isEqualTo("JD mẫu");
        assertThat(jobSkills.findByJobId(job.getId())).extracting(JobSkill::getSkillId).containsExactly(JAVA);
        assertThat(jobs.count()).isEqualTo(1);
    }

    @Test
    void catalogDownIs503AndNothingWritten() throws Exception {
        when(skillCatalog.skillIds(any())).thenThrow(new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE,
                "Không kiểm tra được danh mục kỹ năng, vui lòng thử lại."));
        Job job = saveJob(a.getId(), JobStatus.DRAFT);
        jobSkills.save(new JobSkill(job.getId(), JAVA, true, 60));

        putJob(hr1(), job.getId(), body("title", "Đổi tên", "skills", List.of(skill(SQL, true, 50))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Không kiểm tra được danh mục kỹ năng, vui lòng thử lại."));
        putSkills(hr1(), job.getId(), List.of(skill(SQL, true, 50)))
                .andExpect(status().isServiceUnavailable());
        postJob(hr1(), body("title", "Mới", "skills", List.of(skill(SQL, true, 50))))
                .andExpect(status().isServiceUnavailable());

        assertThat(reload(job).getTitle()).isEqualTo("JD mẫu");
        assertThat(jobSkills.findByJobId(job.getId())).extracting(JobSkill::getSkillId).containsExactly(JAVA);
        assertThat(jobs.count()).isEqualTo(1);

        // Không gửi skills thì không gọi danh mục, lưu bình thường dù profile-service đang lỗi.
        putJob(hr1(), job.getId(), body("title", "Đổi tên")).andExpect(status().isOk());
    }

    @Test
    void invalidSkillPayloadRejected() throws Exception {
        Job job = saveJob(a.getId(), JobStatus.DRAFT);
        jobSkills.save(new JobSkill(job.getId(), JAVA, true, 60));

        List<Map<String, Object>> tooMany = new ArrayList<>();
        for (int i = 0; i < 21; i++) {
            tooMany.add(skill(UUID.randomUUID(), false, 50));
        }
        List<Object> nullElement = new ArrayList<>();
        nullElement.add(null);
        List<List<?>> invalid = List.of(
                List.of(skill(JAVA, true, 0)),
                List.of(skill(JAVA, true, 101)),
                List.of(skill(JAVA, null, 50)),
                List.of(skill(JAVA, true, null)),
                List.of(skill(null, true, 50)),
                List.of(skill(JAVA, true, 50), skill(JAVA, false, 60)),
                nullElement,
                tooMany);
        for (List<?> skills : invalid) {
            putSkills(hr1(), job.getId(), skills)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.skills").exists());
            putJob(hr1(), job.getId(), body("title", "Đổi tên", "skills", skills))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
            postJob(hr1(), body("title", "Mới", "skills", skills))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }

        verify(skillCatalog, never()).skillIds(any());
        assertThat(reload(job).getTitle()).isEqualTo("JD mẫu");
        assertThat(jobSkills.findByJobId(job.getId())).extracting(JobSkill::getSkillId).containsExactly(JAVA);
        assertThat(jobs.count()).isEqualTo(1);

        // Biên hợp lệ: minScore 1 và 100.
        putSkills(hr1(), job.getId(), List.of(skill(JAVA, true, 1), skill(SQL, false, 100)))
                .andExpect(status().isOk());
    }

    @Test
    void hr1EditingCompanyBJobIs404() throws Exception {
        Job jobB = saveJob(b.getId(), JobStatus.DRAFT);
        jobSkills.save(new JobSkill(jobB.getId(), JAVA, true, 60));

        getJob(hr1(), jobB.getId()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy JD"));
        putJob(hr1(), jobB.getId(), body("title", "Hack")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        putSkills(hr1(), jobB.getId(), List.of(skill(SQL, true, 50))).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        // Id không tồn tại cho cùng phản hồi.
        getJob(hr1(), UUID.randomUUID()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Không tìm thấy JD"));

        assertThat(reload(jobB).getTitle()).isEqualTo("JD mẫu");
        assertThat(jobSkills.findByJobId(jobB.getId())).extracting(JobSkill::getSkillId).containsExactly(JAVA);
        getJob(hr2(), jobB.getId()).andExpect(status().isOk());
    }

    @Test
    void otherCompanyCannotReplaceSkills() throws Exception {
        Job jobA = saveJob(a.getId(), JobStatus.DRAFT);
        jobSkills.save(new JobSkill(jobA.getId(), JAVA, true, 60));

        putSkills(hr2(), jobA.getId(), List.of(skill(SQL, true, 50))).andExpect(status().isNotFound());
        putJob(hr2(), jobA.getId(), body("title", "Hack", "skills", List.of(skill(SQL, true, 50))))
                .andExpect(status().isNotFound());

        verify(skillCatalog, never()).skillIds(any());
        List<JobSkill> skills = jobSkills.findByJobId(jobA.getId());
        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).getSkillId()).isEqualTo(JAVA);
        assertThat(skills.get(0).getMinScore()).isEqualTo(60);
        assertThat(reload(jobA).getTitle()).isEqualTo("JD mẫu");
    }

    @Test
    void editPendingOrClosedIs409() throws Exception {
        for (JobStatus s : List.of(JobStatus.PENDING, JobStatus.CLOSED)) {
            Job job = saveJob(a.getId(), s);
            putJob(hr1(), job.getId(), body("title", "Đổi")).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("JOB_NOT_EDITABLE"));
            putSkills(hr1(), job.getId(), List.of(skill(JAVA, true, 50))).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("JOB_NOT_EDITABLE"));
            Job after = reload(job);
            assertThat(after.getTitle()).isEqualTo("JD mẫu");
            assertThat(after.getStatus()).isEqualTo(s);
            assertThat(jobSkills.findByJobId(job.getId())).isEmpty();
        }
        verify(skillCatalog, never()).skillIds(any());
    }

    @Test
    void editApprovedGoesBackToPending() throws Exception {
        Job first = eligibleJob(a.getId(), JobStatus.APPROVED);
        Map<String, Object> req = fullBody();
        req.put("salaryMax", 30_000_000L);
        putJob(hr1(), first.getId(), req).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.salaryMax").value(30_000_000))
                .andExpect(jsonPath("$.reviewedAt").exists());
        Job firstAfter = reload(first);
        assertThat(firstAfter.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(firstAfter.getSalaryMax()).isEqualTo(30_000_000L);
        assertThat(firstAfter.getReviewedAt()).isEqualTo(Instant.parse("2026-11-10T03:00:00Z"));

        Job second = eligibleJob(a.getId(), JobStatus.APPROVED);
        putSkills(hr1(), second.getId(), List.of(skill(JAVA, true, 70), skill(SQL, false, 50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.skills", hasSize(2)));
        assertThat(reload(second).getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void editApprovedRemovingRequiredSkillsIs400AndStaysApproved() throws Exception {
        Job job = eligibleJob(a.getId(), JobStatus.APPROVED);

        putSkills(hr1(), job.getId(), List.of(skill(SQL, false, 50)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.skills").exists());
        Map<String, Object> req = fullBody();
        req.put("skills", List.of());
        putJob(hr1(), job.getId(), req).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.skills").exists());
        Map<String, Object> missingDescription = fullBody();
        missingDescription.remove("description");
        missingDescription.put("title", "Đổi tiêu đề");
        putJob(hr1(), job.getId(), missingDescription).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").exists());

        Job after = reload(job);
        assertThat(after.getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(after.getTitle()).isEqualTo("Lập trình viên Java");
        assertThat(after.getDescription()).isEqualTo("Phát triển API");
        assertThat(jobSkills.findByJobId(job.getId())).extracting(JobSkill::getSkillId).containsExactly(JAVA);
    }

    @Test
    void rejectReasonOnlyShownWhenRejected() throws Exception {
        Job rejected = saveJob(a.getId(), JobStatus.REJECTED);
        rejected.setRejectReason("Thiếu mô tả công việc");
        jobs.save(rejected);
        Job draft = saveJob(a.getId(), JobStatus.DRAFT);
        draft.setRejectReason("Lý do cũ còn sót");
        jobs.save(draft);

        getJob(hr1(), rejected.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("Thiếu mô tả công việc"));
        getJob(hr1(), draft.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.rejectReason").value(nullValue()));

        // Sửa JD bị trả lại: giữ REJECTED và lý do.
        putJob(hr1(), rejected.getId(), body("title", "Đã bổ sung")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("Thiếu mô tả công việc"));
    }

    // ---- danh sách ----

    @Test
    void listMineFiltersAndPages() throws Exception {
        Job draft = saveJob(a.getId(), JobStatus.DRAFT);
        Thread.sleep(5);
        saveJob(a.getId(), JobStatus.PENDING);
        Thread.sleep(5);
        Job approved = saveJob(a.getId(), JobStatus.APPROVED);
        saveJob(b.getId(), JobStatus.DRAFT);

        listMine(hr1(), "?status=DRAFT").andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(draft.getId().toString()))
                .andExpect(jsonPath("$.items[0].title").value("JD mẫu"))
                .andExpect(jsonPath("$.items[0].status").value("DRAFT"))
                .andExpect(jsonPath("$.items[0].updatedAt").exists());
        listMine(hr1(), "").andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].id").value(approved.getId().toString()))
                .andExpect(jsonPath("$.items[2].id").value(draft.getId().toString()));
        listMine(hr1(), "?page=2&size=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(2));
        listMine(hr2(), "").andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        listMine(hr1(), "?page=0").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        listMine(hr1(), "?size=0").andExpect(status().isBadRequest());
        listMine(hr1(), "?size=101").andExpect(status().isBadRequest());
        listMine(hr1(), "?status=XYZ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // ---- quyền ----

    @Test
    void rolesAndLockedCompany() throws Exception {
        Job job = saveJob(a.getId(), JobStatus.DRAFT);

        for (Role role : List.of(Role.STAFF, Role.CANDIDATE)) {
            String auth = bearer(UUID.randomUUID(), role);
            postJob(auth, body("title", "JD")).andExpect(status().isForbidden());
            putJob(auth, job.getId(), body("title", "JD")).andExpect(status().isForbidden());
            putSkills(auth, job.getId(), List.of(skill(JAVA, true, 50))).andExpect(status().isForbidden());
            getJob(auth, job.getId()).andExpect(status().isForbidden());
            listMine(auth, "").andExpect(status().isForbidden());
        }

        // HR bị khoá: 401 ACCOUNT_DISABLED trước khi vào controller.
        UUID lockedHr = UUID.randomUUID();
        linkHr(lockedHr, a.getId());
        CompanyHr link = companyHrs.findById(lockedHr).orElseThrow();
        link.setLockedAt(Instant.parse("2026-11-17T03:00:00Z"));
        companyHrs.save(link);
        getJob(bearer(lockedHr, Role.HR), job.getId()).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
        postJob(bearer(lockedHr, Role.HR), body("title", "JD")).andExpect(status().isUnauthorized());

        // Công ty LOCKED: đọc được, ghi thì 403 COMPANY_LOCKED.
        a.setStatus(CompanyStatus.LOCKED);
        companies.save(a);
        getJob(hr1(), job.getId()).andExpect(status().isOk());
        listMine(hr1(), "").andExpect(status().isOk());
        postJob(hr1(), body("title", "JD")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_LOCKED"));
        putJob(hr1(), job.getId(), body("title", "JD")).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_LOCKED"));
        putSkills(hr1(), job.getId(), List.of(skill(JAVA, true, 50))).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_LOCKED"));
        verify(skillCatalog, never()).skillIds(any());
        assertThat(jobs.count()).isEqualTo(1);
        assertThat(reload(job).getTitle()).isEqualTo("JD mẫu");
    }
}
