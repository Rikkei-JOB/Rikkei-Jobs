package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/** DN-07 §4.2–4.3: trang việc làm công khai và quy tắc "ứng viên thấy JD nào". */
class PublicJobApiTest extends CompanyApiTestSupport {

    private static final UUID JAVA = UUID.fromString("00000000-0000-0000-0001-000000000001");
    private static final UUID SQL = UUID.fromString("00000000-0000-0000-0001-000000000003");

    @Autowired JdbcTemplate jdbc;

    private Company a;
    private Company b;
    private Company c;

    @BeforeEach
    void setUp() {
        a = saveCompany("1111111111", CompanyStatus.ACTIVE);
        a.setLogoUrl("/api/v1/companies/logos/a.png");
        a.setIndustry("Phần mềm");
        a.setCity("Hà Nội");
        a = companies.save(a);
        b = saveCompany("2222222222", CompanyStatus.ACTIVE);
        c = saveCompany("3333333333", CompanyStatus.LOCKED);
        linkHr(HR1, a.getId());
        linkHr(HR2, b.getId());
    }

    // ---- tiện ích ----

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private Job job(Company company, JobStatus status, LocalDate deadline, String title) {
        return job(company, status, deadline, title, Instant.parse("2026-11-01T00:00:00Z"));
    }

    private Job job(Company company, JobStatus status, LocalDate deadline, String title, Instant reviewedAt) {
        Job job = new Job();
        job.setCompanyId(company.getId());
        job.setTitle(title);
        job.setPosition("Backend");
        job.setLevel(JobLevel.JUNIOR);
        job.setType(JobType.FULLTIME);
        job.setWorkMode(WorkMode.HYBRID);
        job.setQuantity(2);
        job.setCity("Hà Nội");
        job.setAddress("Số 1 Cầu Giấy");
        job.setSalaryMin(10_000_000L);
        job.setSalaryMax(20_000_000L);
        job.setDescription("Mô tả");
        job.setRequirements("Yêu cầu");
        job.setBenefits("Quyền lợi");
        job.setDeadline(deadline);
        job.setStatus(status);
        job.setReviewedAt(reviewedAt);
        if (status == JobStatus.REJECTED) {
            job.setRejectReason("Thiếu mô tả");
        }
        job.setCreatedBy(UUID.randomUUID());
        return jobs.save(job);
    }

    private ResultActions list(String query) throws Exception {
        return mvc.perform(get("/jobs" + query));
    }

    private ResultActions detail(UUID id) throws Exception {
        return mvc.perform(get("/jobs/" + id));
    }

    private List<String> listedIds(String query) throws Exception {
        String body = list(query).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.items[*].id");
    }

    // ---- danh sách ----

    @Test
    void onlyVisibleJobsListed() throws Exception {
        LocalDate deadline = today().plusDays(5);
        Job visible = job(a, JobStatus.APPROVED, deadline, "Hiện");
        job(a, JobStatus.PENDING, deadline, "Chờ duyệt");
        job(a, JobStatus.DRAFT, deadline, "Nháp");
        job(a, JobStatus.REJECTED, deadline, "Trả lại");
        job(a, JobStatus.CLOSED, deadline, "Đã đóng");
        job(c, JobStatus.APPROVED, deadline, "Công ty khoá");

        assertThat(listedIds("")).containsExactly(visible.getId().toString());
        list("").andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void deadlineTodayVisibleYesterdayHidden() throws Exception {
        Job dueToday = job(a, JobStatus.APPROVED, today(), "Hạn hôm nay");
        Job dueYesterday = job(a, JobStatus.APPROVED, today().minusDays(1), "Hạn hôm qua");

        assertThat(listedIds("")).containsExactly(dueToday.getId().toString());
        detail(dueToday.getId()).andExpect(status().isOk());
        detail(dueYesterday.getId()).andExpect(status().isNotFound());
    }

    @Test
    void publicListIgnoresMissingOrInvalidToken() throws Exception {
        Job visible = job(a, JobStatus.APPROVED, today().plusDays(1), "Hiện");

        mvc.perform(get("/jobs")).andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)));
        mvc.perform(get("/jobs").header("Authorization", "Bearer abc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)));
        mvc.perform(get("/jobs/" + visible.getId()).header("Authorization", "Bearer abc"))
                .andExpect(status().isOk());
        mvc.perform(get("/jobs").header("Authorization", "Bearer " + token(Role.CANDIDATE)))
                .andExpect(status().isOk());
    }

    @Test
    void sortedByPublishedDescAndPaged() throws Exception {
        LocalDate deadline = today().plusDays(3);
        Job oldest = job(a, JobStatus.APPROVED, deadline, "Cũ", Instant.parse("2026-11-01T00:00:00Z"));
        Job newest = job(b, JobStatus.APPROVED, deadline, "Mới", Instant.parse("2026-11-10T00:00:00Z"));
        Job middle = job(a, JobStatus.APPROVED, deadline, "Giữa", Instant.parse("2026-11-05T00:00:00Z"));

        assertThat(listedIds("")).containsExactly(
                newest.getId().toString(), middle.getId().toString(), oldest.getId().toString());

        list("?size=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.size").value(2));
        assertThat(listedIds("?page=2&size=2")).containsExactly(oldest.getId().toString());

        list("?page=0").andExpect(status().isBadRequest());
        list("?size=101").andExpect(status().isBadRequest());
    }

    @Test
    void listCarriesCompanyRef() throws Exception {
        LocalDate deadline = today().plusDays(3);
        Instant reviewed = Instant.parse("2026-11-10T03:00:00Z");
        Job reviewedJob = job(a, JobStatus.APPROVED, deadline, "Đã duyệt", reviewed);
        Job legacy = job(a, JobStatus.APPROVED, deadline, "Dữ liệu cũ", null);
        Instant updated = Instant.parse("2026-11-12T03:00:00Z");
        jdbc.update("update company.jobs set updated_at = ? where id = ?", Timestamp.from(updated), legacy.getId());

        String body = list("").andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(legacy.getId().toString()))
                .andExpect(jsonPath("$.items[0].company.id").value(a.getId().toString()))
                .andExpect(jsonPath("$.items[0].company.name").value(a.getName()))
                .andExpect(jsonPath("$.items[0].company.logoUrl").value("/api/v1/companies/logos/a.png"))
                .andExpect(jsonPath("$.items[0].title").value("Dữ liệu cũ"))
                .andExpect(jsonPath("$.items[0].level").value("JUNIOR"))
                .andExpect(jsonPath("$.items[0].salaryMin").value(10_000_000))
                .andExpect(jsonPath("$.items[0].deadline").value(deadline.toString()))
                .andExpect(jsonPath("$.items[0]", not(hasKey("status"))))
                .andReturn().getResponse().getContentAsString();

        assertThat(Instant.parse(JsonPath.read(body, "$.items[0].publishedAt"))).isEqualTo(updated);
        assertThat((String) JsonPath.read(body, "$.items[1].id")).isEqualTo(reviewedJob.getId().toString());
        assertThat(Instant.parse(JsonPath.read(body, "$.items[1].publishedAt"))).isEqualTo(reviewed);
    }

    // ---- chi tiết ----

    @Test
    void detailOfVisibleJob() throws Exception {
        Job visible = job(a, JobStatus.APPROVED, today().plusDays(5), "Hiện");
        jobSkills.save(new JobSkill(visible.getId(), SQL, false, 40));
        jobSkills.save(new JobSkill(visible.getId(), JAVA, true, 60));

        detail(visible.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(visible.getId().toString()))
                .andExpect(jsonPath("$.position").value("Backend"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.address").value("Số 1 Cầu Giấy"))
                .andExpect(jsonPath("$.requirements").value("Yêu cầu"))
                .andExpect(jsonPath("$.benefits").value("Quyền lợi"))
                .andExpect(jsonPath("$.publishedAt").exists())
                .andExpect(jsonPath("$.skills", hasSize(2)))
                .andExpect(jsonPath("$.skills[0].skillId").value(JAVA.toString()))
                .andExpect(jsonPath("$.skills[0].required").value(true))
                .andExpect(jsonPath("$.skills[0].minScore").value(60))
                .andExpect(jsonPath("$.company.id").value(a.getId().toString()))
                .andExpect(jsonPath("$.company.industry").value("Phần mềm"))
                .andExpect(jsonPath("$.company.city").value("Hà Nội"))
                .andExpect(jsonPath("$", not(hasKey("status"))))
                .andExpect(jsonPath("$", not(hasKey("rejectReason"))))
                .andExpect(jsonPath("$", not(hasKey("reviewedBy"))))
                .andExpect(jsonPath("$", not(hasKey("createdBy"))));
    }

    @Test
    void detailOfHiddenJobIs404() throws Exception {
        LocalDate deadline = today().plusDays(5);
        Job pending = job(a, JobStatus.PENDING, deadline, "Chờ duyệt");
        Job locked = job(c, JobStatus.APPROVED, deadline, "Công ty khoá");
        Job expired = job(a, JobStatus.APPROVED, today().minusDays(1), "Quá hạn");

        for (UUID id : List.of(pending.getId(), locked.getId(), expired.getId(), UUID.randomUUID())) {
            detail(id).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Không tìm thấy JD"));
        }
    }

    @Test
    void matchEndpointStillNeedsAuth() throws Exception {
        Job visible = job(a, JobStatus.APPROVED, today().plusDays(5), "Hiện");

        mvc.perform(get("/jobs/" + visible.getId() + "/match/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/jobs/" + visible.getId() + "/match/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void hrDetailMovedToCompaniesMe() throws Exception {
        Job draft = job(a, JobStatus.DRAFT, today().plusDays(5), "Nháp");

        mvc.perform(get("/companies/me/jobs/" + draft.getId()).header("Authorization", "Bearer " + token(HR1, Role.HR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draft.getId().toString()))
                .andExpect(jsonPath("$.companyId").value(a.getId().toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"));
        mvc.perform(get("/companies/me/jobs/" + draft.getId()).header("Authorization", "Bearer " + token(HR2, Role.HR)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/companies/me/jobs/" + draft.getId())
                        .header("Authorization", "Bearer " + token(Role.CANDIDATE)))
                .andExpect(status().isForbidden());
    }
}
