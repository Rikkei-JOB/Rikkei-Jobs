package com.rikkei.jobs.company.config;

import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.repository.JobSkillRepository;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Dữ liệu mẫu: Công ty A, Công ty B và liên kết HR theo tài khoản mẫu của identity-service.
 * Chạy lại không tạo bản ghi trùng. Chỉ bật ở dev/test (app.dev-data.enabled=true).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dev-data.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    // Các UUID dưới đây phải khớp với DevDataSeeder của identity-service (company-service không phụ thuộc identity).
    static final UUID COMPANY_A_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID COMPANY_B_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    static final UUID HR1_ID = UUID.fromString("00000000-0000-0000-0001-000000000001");
    static final UUID HR2_ID = UUID.fromString("00000000-0000-0000-0001-000000000002");
    static final UUID HR_NEW_ID = UUID.fromString("00000000-0000-0000-0001-000000000003");
    static final UUID STAFF1_ID = UUID.fromString("00000000-0000-0000-0001-000000000005");

    // UUID kỹ năng cố định của profile-service (V2__skill_catalog.sql, mã đã đổi tên ở V4); seeder không gọi profile-service.
    private static final UUID JAVA = skill(1);
    private static final UUID SPRING_BOOT = skill(2);
    private static final UUID SQL = skill(3);
    private static final UUID GIT = skill(4);
    private static final UUID REST_API = skill(5);
    private static final UUID HTML_CSS = skill(6);
    private static final UUID JAVASCRIPT = skill(7);
    private static final UUID REACTJS = skill(9);
    private static final UUID AGILE = skill(10);
    private static final UUID DOCKER = skill(11);
    private static final UUID OOP = skill(13);
    private static final UUID TESTING = skill(15);
    private static final UUID ENGLISH = skill(17);

    private final CompanyRepository companyRepository;
    private final CompanyHrRepository companyHrRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        company(COMPANY_A_ID, "Công ty A", "0101234567", "Phần mềm", "51-200", "Hà Nội");
        company(COMPANY_B_ID, "Công ty B", "0109876543210", "Thương mại điện tử", "201-1000", "Hồ Chí Minh");
        link(HR1_ID, COMPANY_A_ID);
        link(HR_NEW_ID, COMPANY_A_ID);
        link(HR2_ID, COMPANY_B_ID);
        seedJobs();
    }

    private void company(UUID id, String name, String taxCode, String industry, String size, String city) {
        if (companyRepository.existsById(id)) {
            return;
        }
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        company.setTaxCode(taxCode);
        company.setIndustry(industry);
        company.setSize(size);
        company.setCity(city);
        company.setStatus(CompanyStatus.ACTIVE);
        company.setCreatedBy(STAFF1_ID);
        companyRepository.save(company);
        log.info("Đã tạo công ty mẫu {}", name);
    }

    private void link(UUID userId, UUID companyId) {
        if (companyHrRepository.existsById(userId)) {
            return;
        }
        companyHrRepository.save(new CompanyHr(userId, companyId));
    }

    private static UUID skill(int n) {
        return UUID.fromString(String.format("00000000-0000-0000-0001-%012d", n));
    }

    private static UUID jobId(int n) {
        return UUID.fromString(String.format("00000000-0000-0000-0003-%012d", n));
    }

    /** 7 JD của Công ty A và 2 JD của Công ty B (JD8 APPROVED, JD9 PENDING). Công ty A: 5 JD mỗi trạng thái một JD, thêm 2 JD APPROVED mẫu DN-06; deadline tính theo đồng hồ để seed không hết hạn theo thời gian. */
    private void seedJobs() {
        LocalDate today = JobRules.today(clock);
        Job junior = job(1, "Junior Backend Developer (Java/Spring Boot)", JobLevel.JUNIOR, JobType.FULLTIME,
                JobStatus.REJECTED, today.plusDays(45));
        junior.setRejectReason("Mức lương chưa rõ đơn vị, phần quyền lợi còn sơ sài.");
        saveJob(junior, List.of(
                new JobSkill(null, JAVA, true, 70), new JobSkill(null, OOP, true, 70),
                new JobSkill(null, SPRING_BOOT, true, 40), new JobSkill(null, SQL, true, 40),
                new JobSkill(null, REST_API, true, 40), new JobSkill(null, GIT, true, 70),
                new JobSkill(null, DOCKER, false, 40), new JobSkill(null, ENGLISH, false, 40),
                new JobSkill(null, AGILE, false, 20)));

        saveJob(job(2, "Fresher ReactJS", JobLevel.FRESHER, JobType.FULLTIME, JobStatus.PENDING,
                today.plusDays(45)), List.of(
                new JobSkill(null, REACTJS, true, 40), new JobSkill(null, JAVASCRIPT, true, 40),
                new JobSkill(null, HTML_CSS, true, 40)));

        saveJob(job(3, "Thực tập sinh Tester", JobLevel.INTERN, JobType.INTERNSHIP, JobStatus.APPROVED,
                today.plusDays(45)), List.of(new JobSkill(null, TESTING, true, 40)));

        saveJob(job(4, "Middle Java Developer", JobLevel.MIDDLE, JobType.FULLTIME, JobStatus.DRAFT, null),
                List.of());

        saveJob(job(5, "Senior DevOps", JobLevel.SENIOR, JobType.FULLTIME, JobStatus.CLOSED,
                today.minusDays(10)), List.of());

        // JD6/JD7: hai JD APPROVED để tái hiện ba ví dụ tính tay điểm phù hợp của DN-06.
        saveJob(job(6, "Backend Developer Java (mẫu DN-06)", JobLevel.JUNIOR, JobType.FULLTIME, JobStatus.APPROVED,
                today.plusDays(45)), List.of(
                new JobSkill(null, JAVA, true, 80), new JobSkill(null, SPRING_BOOT, true, 70),
                new JobSkill(null, SQL, true, 90), new JobSkill(null, DOCKER, false, 50),
                new JobSkill(null, ENGLISH, false, 60), new JobSkill(null, GIT, false, 60)));

        saveJob(job(7, "Fresher Java (mẫu DN-06)", JobLevel.FRESHER, JobType.FULLTIME, JobStatus.APPROVED,
                today.plusDays(45)), List.of(
                new JobSkill(null, JAVA, true, 60), new JobSkill(null, SQL, true, 60),
                new JobSkill(null, GIT, true, 60)));

        // JD8/JD9 của Công ty B: một JD đã duyệt (hiện cho ứng viên) và một JD chờ duyệt (cho cán bộ thử duyệt).
        saveJob(job(8, COMPANY_B_ID, HR2_ID, "Thực tập sinh Marketing", JobLevel.INTERN, JobType.INTERNSHIP,
                JobStatus.APPROVED, today.plusDays(45)), List.of(new JobSkill(null, ENGLISH, true, 40)));

        saveJob(job(9, COMPANY_B_ID, HR2_ID, "Nhân viên vận hành sàn TMĐT", JobLevel.JUNIOR, JobType.FULLTIME,
                JobStatus.PENDING, today.plusDays(45)), List.of(new JobSkill(null, ENGLISH, true, 40)));
    }

    private Job job(int n, String title, JobLevel level, JobType type, JobStatus status, LocalDate deadline) {
        return job(n, COMPANY_A_ID, HR1_ID, title, level, type, status, deadline);
    }

    private Job job(int n, UUID companyId, UUID createdBy, String title, JobLevel level, JobType type,
                    JobStatus status, LocalDate deadline) {
        Job job = new Job();
        job.setId(jobId(n));
        job.setCompanyId(companyId);
        job.setTitle(title);
        job.setPosition(title);
        job.setLevel(level);
        job.setType(type);
        job.setQuantity(2);
        job.setCity("Hà Nội");
        job.setAddress("Số 1 Duy Tân, Cầu Giấy, Hà Nội");
        job.setWorkMode(WorkMode.HYBRID);
        job.setSalaryMin(10_000_000L);
        job.setSalaryMax(20_000_000L);
        job.setDescription("Mô tả công việc mẫu cho vị trí " + title + ".");
        job.setRequirements("Yêu cầu mẫu cho vị trí " + title + ".");
        job.setBenefits("Quyền lợi mẫu cho vị trí " + title + ".");
        job.setDeadline(deadline);
        job.setStatus(status);
        job.setCreatedBy(createdBy);
        if (status == JobStatus.APPROVED || status == JobStatus.REJECTED || status == JobStatus.CLOSED) {
            job.setReviewedAt(clock.instant());
        }
        return job;
    }

    /**
     * Lưu JD và kỹ năng của nó trong một transaction: lỗi giữa chừng không để lại JD thiếu kỹ năng
     * (lần chạy sau thấy JD đã có sẽ bỏ qua, không bao giờ bù kỹ năng). Dùng TransactionTemplate vì
     * @Transactional trên method private/gọi nội bộ không có tác dụng.
     */
    private void saveJob(Job job, List<JobSkill> skills) {
        transactionTemplate.executeWithoutResult(status -> {
            if (jobRepository.existsById(job.getId())) {
                return;
            }
            jobRepository.save(job);
            for (JobSkill s : skills) {
                s.setJobId(job.getId());
                jobSkillRepository.save(s);
            }
            log.info("Đã tạo JD mẫu {}", job.getTitle());
        });
    }
}
