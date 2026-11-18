package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

class JobLifecycleApiTest extends CompanyApiTestSupport {

    private static final UUID JAVA = UUID.fromString("00000000-0000-0000-0001-000000000001");

    private Company a;
    private Company b;

    @BeforeEach
    void setUp() {
        a = saveCompany("1111111111", CompanyStatus.ACTIVE);
        b = saveCompany("2222222222", CompanyStatus.ACTIVE);
        linkHr(HR1, a.getId());
        linkHr(HR2, b.getId());
    }

    private String hr1() {
        return "Bearer " + token(HR1, Role.HR);
    }

    private ResultActions submit(UUID id) throws Exception {
        return mvc.perform(post("/jobs/" + id + "/submit").header("Authorization", hr1()));
    }

    private ResultActions close(UUID id) throws Exception {
        return mvc.perform(post("/jobs/" + id + "/close").header("Authorization", hr1()));
    }

    /** JD đủ trường gửi duyệt; chỉ thêm kỹ năng khi {@code required} khác null. */
    private Job job(UUID companyId, JobStatus status, Boolean required) {
        Job job = new Job();
        job.setCompanyId(companyId);
        job.setTitle("Lập trình viên Java");
        job.setPosition("Backend");
        job.setLevel(JobLevel.JUNIOR);
        job.setType(JobType.FULLTIME);
        job.setQuantity(2);
        job.setCity("Hà Nội");
        job.setWorkMode(WorkMode.ONSITE);
        job.setDescription("Phát triển API");
        job.setDeadline(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(10));
        job.setStatus(status);
        job.setCreatedBy(UUID.randomUUID());
        if (status == JobStatus.REJECTED) {
            job.setReviewedBy(UUID.randomUUID());
            job.setReviewedAt(Instant.parse("2026-11-10T03:00:00Z"));
            job.setRejectReason("Thiếu mô tả công việc");
        }
        Job saved = jobs.save(job);
        if (required != null) {
            jobSkills.save(new JobSkill(saved.getId(), JAVA, required, 60));
        }
        return saved;
    }

    private JobStatus statusOf(Job job) {
        return jobs.findById(job.getId()).orElseThrow().getStatus();
    }

    @Test
    void submitWithoutRequiredSkillIs400() throws Exception {
        Job onlyPreferred = job(a.getId(), JobStatus.DRAFT, false);
        submit(onlyPreferred.getId()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.skills").exists());
        assertThat(statusOf(onlyPreferred)).isEqualTo(JobStatus.DRAFT);

        Job none = job(a.getId(), JobStatus.DRAFT, null);
        submit(none.getId()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.skills").exists());
        assertThat(statusOf(none)).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void submitListsAllMissingFields() throws Exception {
        Job job = saveJob(a.getId(), JobStatus.DRAFT);
        submit(job.getId()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.title").doesNotExist())
                .andExpect(jsonPath("$.errors.position").exists())
                .andExpect(jsonPath("$.errors.level").exists())
                .andExpect(jsonPath("$.errors.type").exists())
                .andExpect(jsonPath("$.errors.quantity").exists())
                .andExpect(jsonPath("$.errors.city").exists())
                .andExpect(jsonPath("$.errors.workMode").exists())
                .andExpect(jsonPath("$.errors.deadline").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.skills").exists());
        assertThat(statusOf(job)).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void submitDraftAndRejectedGoPending() throws Exception {
        Job draft = job(a.getId(), JobStatus.DRAFT, true);
        submit(draft.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        assertThat(statusOf(draft)).isEqualTo(JobStatus.PENDING);

        Job rejected = job(a.getId(), JobStatus.REJECTED, true);
        submit(rejected.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.rejectReason").value(nullValue()));
        Job reloaded = jobs.findById(rejected.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(reloaded.getRejectReason()).isNull();
    }

    @Test
    void submitTwiceIs409() throws Exception {
        Job job = job(a.getId(), JobStatus.DRAFT, true);
        submit(job.getId()).andExpect(status().isOk());
        submit(job.getId()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS"));
        assertThat(statusOf(job)).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void submitFromApprovedOrClosedIs409() throws Exception {
        for (JobStatus s : new JobStatus[] {JobStatus.APPROVED, JobStatus.CLOSED}) {
            Job job = job(a.getId(), s, true);
            submit(job.getId()).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS"));
            assertThat(statusOf(job)).isEqualTo(s);
        }
    }

    @Test
    void closeOnlyFromApproved() throws Exception {
        for (JobStatus s : new JobStatus[] {JobStatus.DRAFT, JobStatus.PENDING, JobStatus.REJECTED,
                JobStatus.CLOSED}) {
            Job job = job(a.getId(), s, true);
            close(job.getId()).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS"));
            assertThat(statusOf(job)).isEqualTo(s);
        }
        Job approved = job(a.getId(), JobStatus.APPROVED, true);
        close(approved.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        assertThat(statusOf(approved)).isEqualTo(JobStatus.CLOSED);
    }

    @Test
    void otherCompanyAndLocked() throws Exception {
        Job ofB = job(b.getId(), JobStatus.DRAFT, true);
        submit(ofB.getId()).andExpect(status().isNotFound());
        Job approvedOfB = job(b.getId(), JobStatus.APPROVED, true);
        close(approvedOfB.getId()).andExpect(status().isNotFound());
        assertThat(statusOf(ofB)).isEqualTo(JobStatus.DRAFT);
        assertThat(statusOf(approvedOfB)).isEqualTo(JobStatus.APPROVED);

        Job draft = job(a.getId(), JobStatus.DRAFT, true);
        Job approved = job(a.getId(), JobStatus.APPROVED, true);
        a.setStatus(CompanyStatus.LOCKED);
        companies.save(a);
        submit(draft.getId()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_LOCKED"));
        close(approved.getId()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMPANY_LOCKED"));
        assertThat(statusOf(draft)).isEqualTo(JobStatus.DRAFT);
        assertThat(statusOf(approved)).isEqualTo(JobStatus.APPROVED);
    }
}
