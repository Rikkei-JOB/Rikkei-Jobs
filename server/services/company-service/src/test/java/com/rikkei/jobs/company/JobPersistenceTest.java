package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class JobPersistenceTest extends CompanyApiTestSupport {

    private Job newJob(UUID companyId) {
        Job job = new Job();
        job.setCompanyId(companyId);
        job.setTitle("JD mẫu");
        job.setCreatedBy(UUID.randomUUID());
        return job;
    }

    @Test
    void savesJobWithDefaults() {
        Company c = saveCompany("0100000001", CompanyStatus.ACTIVE);
        Job saved = jobs.saveAndFlush(newJob(c.getId()));

        Job found = jobs.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(JobStatus.DRAFT);
        assertThat(found.getVersion()).isZero();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void rejectsInvalidColumns() {
        Company c = saveCompany("0100000002", CompanyStatus.ACTIVE);
        Job zeroQuantity = newJob(c.getId());
        zeroQuantity.setQuantity(0);
        assertThatThrownBy(() -> jobs.saveAndFlush(zeroQuantity)).isInstanceOf(DataIntegrityViolationException.class);

        Job badRange = newJob(c.getId());
        badRange.setSalaryMin(20L);
        badRange.setSalaryMax(10L);
        assertThatThrownBy(() -> jobs.saveAndFlush(badRange)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void jobSkillRejectsMinScoreZeroAnd101() {
        Company c = saveCompany("0100000003", CompanyStatus.ACTIVE);
        Job job = jobs.saveAndFlush(newJob(c.getId()));
        assertThatThrownBy(() -> jobSkills.saveAndFlush(new JobSkill(job.getId(), UUID.randomUUID(), true, 0)))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jobSkills.saveAndFlush(new JobSkill(job.getId(), UUID.randomUUID(), true, 101)))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void countExcludingDraft() {
        Company a = saveCompany("0100000004", CompanyStatus.ACTIVE);
        saveJob(a.getId(), JobStatus.DRAFT);
        saveJob(a.getId(), JobStatus.PENDING);
        saveJob(a.getId(), JobStatus.APPROVED);

        List<Object[]> rows = jobs.countByCompanyIdsExcludingStatus(List.of(a.getId()), JobStatus.DRAFT);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)[0]).isEqualTo(a.getId());
        assertThat(((Number) rows.get(0)[1]).longValue()).isEqualTo(2L);
    }
}
