package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** DN-07: cán bộ duyệt / trả lại JD. */
class StaffJobReviewApiTest extends CompanyApiTestSupport {

    private static final UUID STAFF = UUID.randomUUID();
    private final LocalDate today = JobRules.today(Clock.systemUTC());
    private Company company;
    private Job pending;

    @BeforeEach
    void seed() {
        company = saveCompany("3000000001", CompanyStatus.ACTIVE);
        pending = job("Java Developer", JobStatus.PENDING, today.plusDays(20));
        jobSkills.save(new JobSkill(pending.getId(), UUID.randomUUID(), true, 60));
        job("Bản nháp", JobStatus.DRAFT, today.plusDays(20));
        job("Đã duyệt", JobStatus.APPROVED, today.plusDays(20));
        Job rejected = job("Bị trả lại", JobStatus.REJECTED, today.plusDays(20));
        rejected.setRejectReason("Thiếu mức lương");
        jobs.save(rejected);
    }

    private Job job(String title, JobStatus status, LocalDate deadline) {
        Job job = saveJob(company.getId(), status);
        job.setTitle(title);
        job.setDeadline(deadline);
        return jobs.save(job);
    }

    private String staff() { return "Bearer " + token(STAFF, Role.STAFF); }

    private ResultActions reject(UUID id, String body) throws Exception {
        return mvc.perform(post("/staff/jobs/" + id + "/reject").header("Authorization", staff())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void listHidesDraftsAndFiltersByStatus() throws Exception {
        mvc.perform(get("/staff/jobs").header("Authorization", staff())).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[*].title", containsInAnyOrder("Java Developer", "Đã duyệt", "Bị trả lại")));
        mvc.perform(get("/staff/jobs").param("status", "PENDING").header("Authorization", staff()))
                .andExpect(jsonPath("$.items[*].title", contains("Java Developer")))
                .andExpect(jsonPath("$.items[0].companyName").value(company.getName()))
                .andExpect(jsonPath("$.items[0].requiredSkillCount").value(1));
        mvc.perform(get("/staff/jobs").param("status", "REJECTED").header("Authorization", staff()))
                .andExpect(jsonPath("$.items[0].rejectReason").value("Thiếu mức lương"));
        mvc.perform(get("/staff/jobs").param("q", "java").header("Authorization", staff())).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/staff/jobs").param("status", "DRAFT").header("Authorization", staff())).andExpect(status().isBadRequest());
        mvc.perform(get("/staff/jobs/summary").header("Authorization", staff()))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.byStatus.PENDING").value(1))
                .andExpect(jsonPath("$.byStatus.CLOSED").value(0))
                .andExpect(jsonPath("$.byStatus.DRAFT").doesNotExist());
    }

    @Test
    void approveMovesPendingToApprovedOnce() throws Exception {
        mvc.perform(post("/staff/jobs/" + pending.getId() + "/approve").header("Authorization", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewedBy").value(STAFF.toString()))
                .andExpect(jsonPath("$.company.name").value(company.getName()));
        Job saved = jobs.findById(pending.getId()).orElseThrow();
        assertThat(saved.getReviewedAt()).isNotNull();
        mvc.perform(post("/staff/jobs/" + pending.getId() + "/approve").header("Authorization", staff()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS"));
    }

    @Test
    void cannotApproveExpiredJob() throws Exception {
        Job expired = job("Hết hạn", JobStatus.PENDING, today.minusDays(1));
        mvc.perform(post("/staff/jobs/" + expired.getId() + "/approve").header("Authorization", staff()))
                .andExpect(status().isConflict());
        assertThat(jobs.findById(expired.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.PENDING);
    }

    @Test
    void rejectNeedsReasonAndHrSeesIt() throws Exception {
        reject(pending.getId(), "{\"reason\":\"   \"}").andExpect(status().isBadRequest());
        reject(pending.getId(), "{\"reason\":\"" + "x".repeat(1001) + "\"}").andExpect(status().isBadRequest());
        reject(pending.getId(), "{\"reason\":\"  Mô tả công việc còn quá ngắn. \"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.job.status").value("REJECTED"))
                .andExpect(jsonPath("$.job.rejectReason").value("Mô tả công việc còn quá ngắn."));
        reject(pending.getId(), "{\"reason\":\"lần hai\"}").andExpect(status().isConflict());
    }

    @Test
    void draftsAreHiddenAndOnlyStaffAllowed() throws Exception {
        Job draft = jobs.findAll().stream().filter(j -> j.getStatus() == JobStatus.DRAFT).findFirst().orElseThrow();
        mvc.perform(get("/staff/jobs/" + draft.getId()).header("Authorization", staff())).andExpect(status().isNotFound());
        mvc.perform(post("/staff/jobs/" + draft.getId() + "/approve").header("Authorization", staff())).andExpect(status().isNotFound());
        mvc.perform(get("/staff/jobs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/staff/jobs").header("Authorization", "Bearer " + token(Role.HR))).andExpect(status().isForbidden());
        // Ma trận quyền NT-01: quản trị cũng duyệt được JD.
        mvc.perform(get("/staff/jobs/summary").header("Authorization", "Bearer " + token(Role.ADMIN))).andExpect(status().isOk());
        mvc.perform(post("/staff/jobs/" + pending.getId() + "/approve").header("Authorization", "Bearer " + token(Role.CANDIDATE)))
                .andExpect(status().isForbidden());
    }
}
