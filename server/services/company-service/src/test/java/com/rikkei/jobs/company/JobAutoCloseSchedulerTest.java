package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.service.JobAutoCloseScheduler;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Clock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/** DN-07 §4.8: mỗi ngày đóng JD APPROVED đã quá hạn nộp, không ghi lịch sử. */
@TestPropertySource(properties = "app.jobs.auto-close.enabled=true")
class JobAutoCloseSchedulerTest extends CompanyApiTestSupport {

    @Autowired JobAutoCloseScheduler scheduler;
    @Autowired Clock clock;

    private Job job(Company c, JobStatus status, LocalDate deadline) {
        Job j = saveJob(c.getId(), status);
        j.setDeadline(deadline);
        return jobs.save(j);
    }

    @Test
    void closesOnlyExpiredApprovedJobs() {
        LocalDate today = JobRules.today(clock);
        Company c = saveCompany("0700000S01", CompanyStatus.ACTIVE);
        Job expired = job(c, JobStatus.APPROVED, today.minusDays(1));
        Job dueToday = job(c, JobStatus.APPROVED, today);
        Job pending = job(c, JobStatus.PENDING, today.minusDays(5));
        long versionBefore = jobs.findById(expired.getId()).orElseThrow().getVersion();

        assertThat(scheduler.closeExpired()).isEqualTo(1);

        Job closed = jobs.findById(expired.getId()).orElseThrow();
        assertThat(closed.getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(closed.getVersion()).isGreaterThan(versionBefore);
        assertThat(jobs.findById(dueToday.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(jobs.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(companyActions.count()).isZero();
        assertThat(scheduler.closeExpired()).isZero();
    }
}
