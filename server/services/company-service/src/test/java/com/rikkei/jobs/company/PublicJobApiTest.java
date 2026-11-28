package com.rikkei.jobs.company;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.entity.WorkMode;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Bảng việc làm công khai: không cần token, chỉ tin APPROVED còn hạn của công ty ACTIVE. */
class PublicJobApiTest extends CompanyApiTestSupport {

    private static final UUID JAVA = UUID.fromString("00000000-0000-0000-0001-000000000001");
    private static final UUID REACT = UUID.fromString("00000000-0000-0000-0001-000000000002");

    private final LocalDate today = JobRules.today(Clock.systemUTC());
    private Company active;
    private Job backend;
    private Job frontend;

    @BeforeEach
    void seed() {
        active = saveCompany("2000000001", CompanyStatus.ACTIVE);
        active.setName("Công ty Sông Hồng");
        active.setCity("Hà Nội");
        companies.save(active);
        Company locked = saveCompany("2000000002", CompanyStatus.LOCKED);

        backend = job(active, "Java Backend Developer", JobStatus.APPROVED, today.plusDays(10), 15_000_000L, 25_000_000L);
        backend.setLevel(JobLevel.JUNIOR);
        backend.setWorkMode(WorkMode.HYBRID);
        backend.setCity("Hà Nội");
        jobs.save(backend);
        jobSkills.save(new JobSkill(backend.getId(), JAVA, true, 60));

        frontend = job(active, "Frontend Engineer", JobStatus.APPROVED, today, 30_000_000L, 45_000_000L);
        frontend.setLevel(JobLevel.MIDDLE);
        frontend.setCity("TP. Hồ Chí Minh");
        jobs.save(frontend);
        jobSkills.save(new JobSkill(frontend.getId(), REACT, true, 50));

        job(active, "Tin nháp", JobStatus.DRAFT, today.plusDays(5), null, null);
        job(active, "Tin chờ duyệt", JobStatus.PENDING, today.plusDays(5), null, null);
        job(active, "Tin hết hạn", JobStatus.APPROVED, today.minusDays(1), null, null);
        job(locked, "Tin của công ty bị khoá", JobStatus.APPROVED, today.plusDays(5), null, null);
    }

    private Job job(Company company, String title, JobStatus status, LocalDate deadline, Long min, Long max) {
        Job job = saveJob(company.getId(), status);
        job.setTitle(title);
        job.setDeadline(deadline);
        job.setSalaryMin(min);
        job.setSalaryMax(max);
        return jobs.save(job);
    }

    @Test
    void listShowsOnlyVisibleJobsWithoutToken() throws Exception {
        mvc.perform(get("/public/jobs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[*].title", containsInAnyOrder("Java Backend Developer", "Frontend Engineer")))
                .andExpect(jsonPath("$.items[0].company.name").value("Công ty Sông Hồng"))
                .andExpect(jsonPath("$.items[0].company.taxCode").doesNotExist())
                .andExpect(jsonPath("$.items[0].status").doesNotExist());
    }

    /** DN-07: token sai hay token hợp lệ của ứng viên đều không chặn trang công khai. */
    @Test
    void publicListIgnoresMissingOrInvalidToken() throws Exception {
        mvc.perform(get("/public/jobs").header("Authorization", "Bearer abc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/public/jobs/" + backend.getId()).header("Authorization", "Bearer abc"))
                .andExpect(status().isOk());
        mvc.perform(get("/public/jobs").header("Authorization", "Bearer " + token(Role.CANDIDATE)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void keywordMatchesTitleCompanyOrSkill() throws Exception {
        mvc.perform(get("/public/jobs").param("q", "BACKEND"))
                .andExpect(jsonPath("$.items[*].title", contains("Java Backend Developer")));
        mvc.perform(get("/public/jobs").param("q", "sông hồng")).andExpect(jsonPath("$.total").value(2));
        // Từ khoá không khớp chữ nào nhưng frontend gửi kèm id kỹ năng React.
        mvc.perform(get("/public/jobs").param("q", "reactjs").param("skillIds", REACT.toString()))
                .andExpect(jsonPath("$.items[*].title", contains("Frontend Engineer")))
                .andExpect(jsonPath("$.items[0].skills[0].skillId").value(REACT.toString()));
        mvc.perform(get("/public/jobs").param("q", "100%")).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void filtersCityLevelModeAndSalary() throws Exception {
        mvc.perform(get("/public/jobs").param("city", "hồ chí minh"))
                .andExpect(jsonPath("$.items[*].title", contains("Frontend Engineer")));
        mvc.perform(get("/public/jobs").param("level", "JUNIOR").param("workMode", "HYBRID"))
                .andExpect(jsonPath("$.items[*].title", contains("Java Backend Developer")));
        mvc.perform(get("/public/jobs").param("salaryFrom", "30000000"))
                .andExpect(jsonPath("$.items[*].title", contains("Frontend Engineer")));
        mvc.perform(get("/public/jobs").param("salaryFrom", "15000000").param("salaryTo", "30000000"))
                .andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/public/jobs").param("companyId", UUID.randomUUID().toString()))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void sortsBySalaryAndPaginates() throws Exception {
        mvc.perform(get("/public/jobs").param("sort", "salary"))
                .andExpect(jsonPath("$.items[*].title", contains("Frontend Engineer", "Java Backend Developer")));
        mvc.perform(get("/public/jobs").param("size", "1").param("page", "2"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.page").value(2));
        mvc.perform(get("/public/jobs").param("sort", "random")).andExpect(status().isBadRequest());
        mvc.perform(get("/public/jobs").param("size", "101")).andExpect(status().isBadRequest());
        mvc.perform(get("/public/jobs").param("level", "BOSS")).andExpect(status().isBadRequest());
    }

    @Test
    void detailHidesJobsCandidatesCannotSee() throws Exception {
        mvc.perform(get("/public/jobs/" + backend.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Java Backend Developer"))
                .andExpect(jsonPath("$.company.id").value(active.getId().toString()))
                .andExpect(jsonPath("$.skills[0].required").value(true))
                .andExpect(jsonPath("$.rejectReason").doesNotExist());
        for (Job hidden : jobs.findAll()) {
            if (!hidden.getId().equals(backend.getId()) && !hidden.getId().equals(frontend.getId())) {
                mvc.perform(get("/public/jobs/" + hidden.getId())).andExpect(status().isNotFound());
            }
        }
    }

    @Test
    void companiesListActiveOnesWithOpenJobCounts() throws Exception {
        saveCompany("2000000003", CompanyStatus.ACTIVE);
        mvc.perform(get("/public/companies")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Công ty Sông Hồng"))
                .andExpect(jsonPath("$[0].openJobs").value(2))
                .andExpect(jsonPath("$[1].openJobs").value(0));
        mvc.perform(get("/public/companies/" + active.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.openJobs").value(2))
                .andExpect(jsonPath("$.taxCode").doesNotExist());
        Company locked = companies.findAll().stream().filter(c -> c.getStatus() == CompanyStatus.LOCKED).findFirst().orElseThrow();
        mvc.perform(get("/public/companies/" + locked.getId())).andExpect(status().isNotFound());
    }
}
