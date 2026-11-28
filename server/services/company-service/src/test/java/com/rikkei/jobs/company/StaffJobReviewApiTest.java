package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyAction;
import com.rikkei.jobs.company.entity.CompanyActionType;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** DN-07: cán bộ duyệt / trả lại / đóng JD, mỗi thao tác ghi lịch sử công ty. */
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

    private ResultActions close(UUID id, String body) throws Exception {
        return mvc.perform(post("/staff/jobs/" + id + "/close").header("Authorization", staff())
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_EXPIRED"));
        assertThat(jobs.findById(expired.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(companyActions.count()).isZero();

        // Hạn nộp đúng hôm nay (giờ Việt Nam) vẫn duyệt được.
        Job dueToday = job("Hạn hôm nay", JobStatus.PENDING, today);
        mvc.perform(post("/staff/jobs/" + dueToday.getId() + "/approve").header("Authorization", staff()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.job.status").value("APPROVED"));
    }

    /** DN-07: duyệt ghi một dòng lịch sử APPROVE_JD (không lý do) và JD lên trang công khai. */
    @Test
    void approveWritesHistoryAndPublishes() throws Exception {
        mvc.perform(post("/staff/jobs/" + pending.getId() + "/approve").header("Authorization", staff()))
                .andExpect(status().isOk());

        List<CompanyAction> history = companyActions.findAll();
        assertThat(history).hasSize(1);
        CompanyAction row = history.get(0);
        assertThat(row.getAction()).isEqualTo(CompanyActionType.APPROVE_JD);
        assertThat(row.getReason()).isNull();
        assertThat(row.getTargetId()).isEqualTo(pending.getId());
        assertThat(row.getCompanyId()).isEqualTo(company.getId());
        assertThat(row.getStaffId()).isEqualTo(STAFF);

        mvc.perform(get("/public/jobs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].title", hasItem("Java Developer")));
    }

    @Test
    void rejectNeedsReasonAndHrSeesIt() throws Exception {
        reject(pending.getId(), "{\"reason\":\"   \"}").andExpect(status().isBadRequest());
        reject(pending.getId(), "{\"reason\":\"" + "x".repeat(1001) + "\"}").andExpect(status().isBadRequest());
        reject(pending.getId(), "{\"reason\":\"  Mô tả công việc còn quá ngắn. \"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.job.status").value("REJECTED"))
                .andExpect(jsonPath("$.job.rejectReason").value("Mô tả công việc còn quá ngắn."));
        reject(pending.getId(), "{\"reason\":\"lần hai\"}").andExpect(status().isConflict());

        List<CompanyAction> history = companyActions.findAll();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(CompanyActionType.REJECT_JD);
        assertThat(history.get(0).getReason()).isEqualTo("Mô tả công việc còn quá ngắn.");
        assertThat(history.get(0).getTargetId()).isEqualTo(pending.getId());
        assertThat(history.get(0).getStaffId()).isEqualTo(STAFF);
    }

    /** DN-07: lý do chỉ có khoảng trắng (kể cả khoảng trắng không ngắt) là thiếu; độ dài tính sau khi trim. */
    @Test
    void blankReasonRejected() throws Exception {
        Job approved = jobs.findAll().stream().filter(j -> j.getStatus() == JobStatus.APPROVED).findFirst().orElseThrow();
        for (String blank : List.of("   ", "    ")) {
            String body = "{\"reason\":\"" + blank + "\"}";
            reject(pending.getId(), body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.reason").exists());
            close(approved.getId(), body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.reason").exists());
        }
        assertThat(jobs.findById(pending.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(jobs.findById(approved.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(companyActions.count()).isZero();

        String max = "x".repeat(1000);
        reject(pending.getId(), "{\"reason\":\"  " + max + "  \"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.job.rejectReason").value(max));
        assertThat(companyActions.findAll().get(0).getReason()).isEqualTo(max);
    }

    /** DN-07: đóng JD đang đăng (chỉ APPROVED), lý do bắt buộc, ghi lịch sử CLOSE_JD và gỡ khỏi trang công khai. */
    @Test
    void closeApprovedOnlyWritesHistory() throws Exception {
        Job approved = jobs.findAll().stream().filter(j -> j.getStatus() == JobStatus.APPROVED).findFirst().orElseThrow();
        close(approved.getId(), "{}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
        close(approved.getId(), "{\"reason\":\"" + "x".repeat(1001) + "\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
        assertThat(companyActions.count()).isZero();

        close(approved.getId(), "{\"reason\":\" Vi phạm quy định đăng tin \"}").andExpect(status().isOk())
                .andExpect(jsonPath("$.job.status").value("CLOSED"))
                .andExpect(jsonPath("$.reviewedBy").value(STAFF.toString()));
        assertThat(jobs.findById(approved.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.CLOSED);
        List<CompanyAction> history = companyActions.findAll();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(CompanyActionType.CLOSE_JD);
        assertThat(history.get(0).getReason()).isEqualTo("Vi phạm quy định đăng tin");
        assertThat(history.get(0).getTargetId()).isEqualTo(approved.getId());
        assertThat(history.get(0).getStaffId()).isEqualTo(STAFF);
        mvc.perform(get("/public/jobs/" + approved.getId())).andExpect(status().isNotFound());

        for (JobStatus s : List.of(JobStatus.PENDING, JobStatus.REJECTED, JobStatus.CLOSED)) {
            Job other = jobs.findAll().stream().filter(j -> j.getStatus() == s).findFirst().orElseThrow();
            close(other.getId(), "{\"reason\":\"Vi phạm\"}").andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS"));
            assertThat(jobs.findById(other.getId()).orElseThrow().getStatus()).isEqualTo(s);
        }
        close(UUID.randomUUID(), "{\"reason\":\"Vi phạm\"}").andExpect(status().isNotFound());
        assertThat(companyActions.count()).isEqualTo(1);
    }

    @Test
    void draftsAreHiddenAndOnlyStaffAllowed() throws Exception {
        Job draft = jobs.findAll().stream().filter(j -> j.getStatus() == JobStatus.DRAFT).findFirst().orElseThrow();
        // Xem chi tiết JD nháp: cán bộ không thấy bản nháp nên vẫn 404.
        mvc.perform(get("/staff/jobs/" + draft.getId()).header("Authorization", staff())).andExpect(status().isNotFound());
        // DN-07: JD nháp vẫn tồn tại nên thao tác duyệt / trả lại / đóng trả 409, không phải 404.
        mvc.perform(post("/staff/jobs/" + draft.getId() + "/approve").header("Authorization", staff()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS"));
        reject(draft.getId(), "{\"reason\":\"Thiếu mức lương\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS"));
        close(draft.getId(), "{\"reason\":\"Vi phạm\"}")
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS"));
        assertThat(jobs.findById(draft.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.DRAFT);
        // id không có trong DB mới là 404.
        mvc.perform(post("/staff/jobs/" + UUID.randomUUID() + "/approve").header("Authorization", staff()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/staff/jobs")).andExpect(status().isUnauthorized());
        mvc.perform(get("/staff/jobs").header("Authorization", "Bearer " + token(Role.HR))).andExpect(status().isForbidden());
        // Ma trận quyền NT-01: quản trị cũng duyệt được JD.
        mvc.perform(get("/staff/jobs/summary").header("Authorization", "Bearer " + token(Role.ADMIN))).andExpect(status().isOk());
        mvc.perform(post("/staff/jobs/" + pending.getId() + "/approve").header("Authorization", "Bearer " + token(Role.CANDIDATE)))
                .andExpect(status().isForbidden());
        Job approved = jobs.findAll().stream().filter(j -> j.getStatus() == JobStatus.APPROVED).findFirst().orElseThrow();
        mvc.perform(post("/staff/jobs/" + approved.getId() + "/close").header("Authorization", "Bearer " + token(Role.HR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Vi phạm\"}"))
                .andExpect(status().isForbidden());
        assertThat(jobs.findById(approved.getId()).orElseThrow().getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(companyActions.count()).isZero();
    }
}
