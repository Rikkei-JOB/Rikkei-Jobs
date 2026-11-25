package com.rikkei.jobs.company;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/** DN-05: tổng hợp JD theo trạng thái và dòng danh sách JD giàu thông tin hơn. */
class JobSummaryApiTest extends CompanyApiTestSupport {

    private static final String SUMMARY = "/companies/me/jobs/summary";

    private ResultActions summary(String token) throws Exception {
        return mvc.perform(get(SUMMARY).header("Authorization", "Bearer " + token));
    }

    private Job saveJob(Company company, JobStatus status, LocalDate deadline) {
        Job job = saveJob(company.getId(), status);
        job.setDeadline(deadline);
        return jobs.save(job);
    }

    @Test
    void summaryCountsEveryStatus() throws Exception {
        Company a = saveCompany("1000000001", CompanyStatus.ACTIVE);
        Company b = saveCompany("1000000002", CompanyStatus.ACTIVE);
        linkHr(HR1, a.getId());
        saveJob(a.getId(), JobStatus.DRAFT);
        saveJob(a.getId(), JobStatus.DRAFT);
        saveJob(a.getId(), JobStatus.PENDING);
        saveJob(a.getId(), JobStatus.APPROVED);
        saveJob(a.getId(), JobStatus.REJECTED);
        for (int i = 0; i < 3; i++) {
            saveJob(b.getId(), JobStatus.APPROVED);
        }

        summary(token(HR1, Role.HR)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.byStatus.DRAFT").value(2))
                .andExpect(jsonPath("$.byStatus.PENDING").value(1))
                .andExpect(jsonPath("$.byStatus.APPROVED").value(1))
                .andExpect(jsonPath("$.byStatus.REJECTED").value(1))
                .andExpect(jsonPath("$.byStatus.CLOSED").value(0))
                .andExpect(jsonPath("$.expiringSoon").value(0));
    }

    @Test
    void expiringSoonBoundaries() throws Exception {
        Company a = saveCompany("1000000003", CompanyStatus.ACTIVE);
        linkHr(HR1, a.getId());
        LocalDate today = JobRules.today(Clock.systemUTC());
        saveJob(a, JobStatus.APPROVED, today);
        saveJob(a, JobStatus.APPROVED, today.plusDays(7));
        saveJob(a, JobStatus.APPROVED, today.plusDays(8));
        saveJob(a, JobStatus.APPROVED, today.minusDays(1));
        saveJob(a, JobStatus.APPROVED, null);
        saveJob(a, JobStatus.PENDING, today.plusDays(1));

        summary(token(HR1, Role.HR)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(6))
                .andExpect(jsonPath("$.expiringSoon").value(2));
    }

    @Test
    void summaryForLockedCompanyStillReturns() throws Exception {
        Company a = saveCompany("1000000004", CompanyStatus.LOCKED);
        linkHr(HR1, a.getId());
        saveJob(a.getId(), JobStatus.DRAFT);

        summary(token(HR1, Role.HR)).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void summaryWithoutCompanyIs404AndOtherRolesForbidden() throws Exception {
        summary(token(HR2, Role.HR)).andExpect(status().isNotFound());
        summary(token(Role.STAFF)).andExpect(status().isForbidden());
    }

    @Test
    void listIncludesRequiredSkillCountAndRejectReason() throws Exception {
        Company a = saveCompany("1000000005", CompanyStatus.ACTIVE);
        linkHr(HR1, a.getId());
        Job withSkills = saveJob(a.getId(), JobStatus.DRAFT);
        jobSkills.save(new JobSkill(withSkills.getId(), UUID.randomUUID(), true, 50));
        jobSkills.save(new JobSkill(withSkills.getId(), UUID.randomUUID(), true, 50));
        jobSkills.save(new JobSkill(withSkills.getId(), UUID.randomUUID(), false, 50));
        Job rejected = saveJob(a.getId(), JobStatus.REJECTED);
        rejected.setRejectReason("Thiếu lương");
        jobs.save(rejected);
        Job bare = saveJob(a.getId(), JobStatus.PENDING);

        mvc.perform(get("/companies/me/jobs").header("Authorization", "Bearer " + token(HR1, Role.HR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[?(@.id=='" + withSkills.getId() + "')].requiredSkillCount")
                        .value(2))
                .andExpect(jsonPath("$.items[?(@.id=='" + withSkills.getId() + "')].rejectReason")
                        .value((Object) null))
                .andExpect(jsonPath("$.items[?(@.id=='" + rejected.getId() + "')].rejectReason")
                        .value("Thiếu lương"))
                .andExpect(jsonPath("$.items[?(@.id=='" + rejected.getId() + "')].requiredSkillCount")
                        .value(0))
                .andExpect(jsonPath("$.items[?(@.id=='" + bare.getId() + "')].requiredSkillCount")
                        .value(0));
    }
}
