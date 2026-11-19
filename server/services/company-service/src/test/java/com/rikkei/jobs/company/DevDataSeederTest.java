package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkei.jobs.company.config.DevDataSeeder;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.entity.JobType;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/*
 * Database H2 riêng: seed chạy ngay khi context khởi động, trước cleanTables(). Nếu dùng chung DB "company" với
 * các lớp test khác, dữ liệu chúng để lại (ví dụ công ty có mã số thuế 0101234567) làm seed vi phạm UNIQUE và
 * context không lên. Lỗi chỉ lộ ra theo thứ tự chạy lớp (Linux trên CI khác Windows).
 */
@TestPropertySource(properties = {
    "app.dev-data.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:company-seeder;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE"
})
class DevDataSeederTest extends CompanyApiTestSupport {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID HR_NEW = UUID.fromString("00000000-0000-0000-0001-000000000003");
    private static final UUID STAFF1 = UUID.fromString("00000000-0000-0000-0001-000000000005");

    @Autowired DevDataSeeder seeder;

    @Test
    void seedsSampleCompaniesAndHrLinksAndIsIdempotent() throws Exception {
        // cleanTables() của lớp nền đã xoá dữ liệu do runner chạy lúc khởi động; seed lại từ đầu.
        seeder.run(null);
        assertSeeded();

        seeder.run(null);
        assertSeeded();
    }

    private void assertSeededJobs() {
        assertThat(jobs.count()).isEqualTo(7);
        List<Job> aJobs = jobs.findAll();
        assertThat(aJobs).allMatch(j -> A.equals(j.getCompanyId()) && HR1.equals(j.getCreatedBy()));
        assertThat(aJobs).extracting(Job::getStatus).containsExactlyInAnyOrder(
                JobStatus.REJECTED, JobStatus.PENDING, JobStatus.APPROVED, JobStatus.DRAFT, JobStatus.CLOSED,
                JobStatus.APPROVED, JobStatus.APPROVED);

        Job backend = jobs.findById(jobId(6)).orElseThrow();
        assertThat(backend.getStatus()).isEqualTo(JobStatus.APPROVED);
        List<JobSkill> backendSkills = jobSkills.findByJobId(backend.getId());
        assertThat(backendSkills).hasSize(6);
        assertThat(backendSkills.stream().filter(JobSkill::isRequired).count()).isEqualTo(3);
        assertThat(backendSkills).filteredOn(s -> s.getSkillId().equals(
                UUID.fromString("00000000-0000-0000-0001-000000000003"))).singleElement()
                .satisfies(s -> assertThat(s.getMinScore()).isEqualTo(90));

        Job fresher = jobs.findById(jobId(7)).orElseThrow();
        assertThat(fresher.getStatus()).isEqualTo(JobStatus.APPROVED);
        List<JobSkill> fresherSkills = jobSkills.findByJobId(fresher.getId());
        assertThat(fresherSkills).hasSize(3);
        assertThat(fresherSkills).allMatch(s -> s.isRequired() && s.getMinScore() == 60);

        Job junior = jobs.findById(jobId(1)).orElseThrow();
        assertThat(junior.getTitle()).isEqualTo("Junior Backend Developer (Java/Spring Boot)");
        assertThat(junior.getStatus()).isEqualTo(JobStatus.REJECTED);
        assertThat(junior.getRejectReason()).isEqualTo("Mức lương chưa rõ đơn vị, phần quyền lợi còn sơ sài.");
        List<JobSkill> skills = jobSkills.findByJobId(junior.getId());
        assertThat(skills).hasSize(9);
        assertThat(skills.stream().filter(JobSkill::isRequired).count()).isEqualTo(6);

        Job tester = jobs.findById(jobId(3)).orElseThrow();
        assertThat(tester.getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(tester.getType()).isEqualTo(JobType.INTERNSHIP);
        assertThat(tester.getDeadline()).isAfter(LocalDate.now().plusDays(30));

        Job draft = jobs.findById(jobId(4)).orElseThrow();
        assertThat(draft.getDeadline()).isNull();
        assertThat(jobSkills.findByJobId(draft.getId())).isEmpty();
        assertThat(jobs.findById(jobId(5)).orElseThrow().getDeadline()).isBefore(LocalDate.now());
    }

    private static UUID jobId(int n) {
        return UUID.fromString("00000000-0000-0000-0003-00000000000" + n);
    }

    private void assertSeeded() {
        assertSeededJobs();
        assertThat(companies.count()).isEqualTo(2);
        assertThat(companyHrs.count()).isEqualTo(3);

        Company a = companies.findById(A).orElseThrow();
        assertThat(a.getName()).isEqualTo("Công ty A");
        assertThat(a.getTaxCode()).isEqualTo("0101234567");
        assertThat(a.getIndustry()).isEqualTo("Phần mềm");
        assertThat(a.getSize()).isEqualTo("51-200");
        assertThat(a.getCity()).isEqualTo("Hà Nội");
        assertThat(a.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(a.getCreatedBy()).isEqualTo(STAFF1);

        Company b = companies.findById(B).orElseThrow();
        assertThat(b.getName()).isEqualTo("Công ty B");
        assertThat(b.getTaxCode()).isEqualTo("0109876543210");
        assertThat(b.getIndustry()).isEqualTo("Thương mại điện tử");
        assertThat(b.getSize()).isEqualTo("201-1000");
        assertThat(b.getCity()).isEqualTo("Hồ Chí Minh");
        assertThat(b.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(b.getCreatedBy()).isEqualTo(STAFF1);

        assertThat(companyHrs.findById(HR1).orElseThrow().getCompanyId()).isEqualTo(A);
        assertThat(companyHrs.findById(HR_NEW).orElseThrow().getCompanyId()).isEqualTo(A);
        assertThat(companyHrs.findById(HR2).orElseThrow().getCompanyId()).isEqualTo(B);
    }
}
