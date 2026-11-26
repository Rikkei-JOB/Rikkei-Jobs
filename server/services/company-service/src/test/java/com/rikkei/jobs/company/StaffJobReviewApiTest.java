package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyAction;
import com.rikkei.jobs.company.entity.CompanyActionType;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

/** DN-07 §4.4: cán bộ xem hàng chờ, duyệt / trả lại / đóng JD. */
class StaffJobReviewApiTest extends CompanyApiTestSupport {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final UUID STAFF = UUID.fromString("00000000-0000-0000-0002-000000000001");
    private static final UUID JAVA = UUID.fromString("00000000-0000-0000-0001-000000000001");

    @Autowired JdbcTemplate jdbc;

    private Company a;
    private Company b;

    @BeforeEach
    void setUp() {
        a = saveCompany("1111111111", CompanyStatus.ACTIVE);
        b = saveCompany("2222222222", CompanyStatus.ACTIVE);
        linkHr(HR1, a.getId());
        linkHr(HR2, b.getId());
        when(identity.lookupUsers(any())).thenReturn(List.of(
                new IdentityUserStatus(STAFF, "ha.ht@rikkei.vn", "Hoàng Thu Hà", "ACTIVE", null, null, null)));
    }

    // ---- tiện ích ----

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private String staff() {
        return "Bearer " + token(STAFF, Role.STAFF);
    }

    private String hr1() {
        return "Bearer " + token(HR1, Role.HR);
    }

    /** JD đủ trường gửi duyệt, hạn nộp 10 ngày nữa. */
    private Job job(Company company, JobStatus status, String title) {
        return job(company, status, title, today().plusDays(10));
    }

    private Job job(Company company, JobStatus status, String title, LocalDate deadline) {
        Job job = new Job();
        job.setCompanyId(company.getId());
        job.setTitle(title);
        job.setPosition("Backend");
        job.setLevel(JobLevel.JUNIOR);
        job.setType(JobType.FULLTIME);
        job.setWorkMode(WorkMode.ONSITE);
        job.setQuantity(2);
        job.setCity("Hà Nội");
        job.setDescription("Phát triển API");
        job.setDeadline(deadline);
        job.setStatus(status);
        job.setCreatedBy(UUID.randomUUID());
        if (status == JobStatus.REJECTED) {
            job.setReviewedBy(STAFF);
            job.setReviewedAt(Instant.parse("2026-11-10T03:00:00Z"));
            job.setRejectReason("Thiếu mô tả công việc");
        }
        Job saved = jobs.save(job);
        jobSkills.save(new JobSkill(saved.getId(), JAVA, true, 60));
        return saved;
    }

    private void setUpdatedAt(Job job, String instant) {
        jdbc.update("update company.jobs set updated_at = ? where id = ?",
                Timestamp.from(Instant.parse(instant)), job.getId());
    }

    private ResultActions list(String query) throws Exception {
        return mvc.perform(get("/staff/jobs" + query).header("Authorization", staff()));
    }

    private List<String> listedIds(String query) throws Exception {
        String body = list(query).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.items[*].id");
    }

    private ResultActions detail(UUID id) throws Exception {
        return mvc.perform(get("/staff/jobs/" + id).header("Authorization", staff()));
    }

    private ResultActions approve(UUID id) throws Exception {
        return mvc.perform(post("/staff/jobs/" + id + "/approve").header("Authorization", staff()));
    }

    private ResultActions withReason(String action, UUID id, String auth, Object body) throws Exception {
        return mvc.perform(post("/staff/jobs/" + id + "/" + action).header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    private ResultActions reject(UUID id, Object body) throws Exception {
        return withReason("reject", id, staff(), body);
    }

    private ResultActions close(UUID id, Object body) throws Exception {
        return withReason("close", id, staff(), body);
    }

    private static Map<String, Object> reason(String reason) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reason", reason);
        return m;
    }

    private Job reload(Job job) {
        return jobs.findById(job.getId()).orElseThrow();
    }

    // ---- danh sách ----

    @Test
    void pendingQueueOldestFirst() throws Exception {
        Job late = job(a, JobStatus.PENDING, "Gửi muộn");
        Job early = job(a, JobStatus.PENDING, "Gửi sớm");
        Job middle = job(b, JobStatus.PENDING, "Gửi giữa");
        setUpdatedAt(late, "2026-11-03T00:00:00Z");
        setUpdatedAt(early, "2026-11-01T00:00:00Z");
        setUpdatedAt(middle, "2026-11-02T00:00:00Z");
        Job approvedOld = job(a, JobStatus.APPROVED, "Duyệt cũ");
        Job approvedNew = job(b, JobStatus.APPROVED, "Duyệt mới");
        setUpdatedAt(approvedOld, "2026-11-01T00:00:00Z");
        setUpdatedAt(approvedNew, "2026-11-05T00:00:00Z");
        job(a, JobStatus.DRAFT, "Nháp");

        assertThat(listedIds("?status=PENDING")).containsExactly(
                early.getId().toString(), middle.getId().toString(), late.getId().toString());
        list("?status=PENDING")
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].title").value("Gửi sớm"))
                .andExpect(jsonPath("$.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.items[0].deadline").value(today().plusDays(10).toString()))
                .andExpect(jsonPath("$.items[0].submittedAt").value("2026-11-01T00:00:00Z"))
                .andExpect(jsonPath("$.items[0].company.id").value(a.getId().toString()))
                .andExpect(jsonPath("$.items[0].company.name").value(a.getName()));

        assertThat(listedIds("?status=PENDING&companyId=" + b.getId())).containsExactly(middle.getId().toString());
        assertThat(listedIds("?status=APPROVED")).containsExactly(
                approvedNew.getId().toString(), approvedOld.getId().toString());

        list("?status=XYZ").andExpect(status().isBadRequest());
        list("?status=PENDING&companyId=abc").andExpect(status().isBadRequest());
        list("?status=PENDING&page=0").andExpect(status().isBadRequest());
        list("?status=PENDING&size=101").andExpect(status().isBadRequest());
    }

    @Test
    void omittedStatusListsAllOfCompany() throws Exception {
        Job draft = job(a, JobStatus.DRAFT, "Nháp");
        Job pending = job(a, JobStatus.PENDING, "Chờ duyệt");
        Job approved = job(a, JobStatus.APPROVED, "Đã duyệt");
        Job rejected = job(a, JobStatus.REJECTED, "Trả lại");
        setUpdatedAt(draft, "2026-11-04T00:00:00Z");
        setUpdatedAt(pending, "2026-11-01T00:00:00Z");
        setUpdatedAt(approved, "2026-11-03T00:00:00Z");
        setUpdatedAt(rejected, "2026-11-02T00:00:00Z");
        job(b, JobStatus.PENDING, "Công ty khác");

        assertThat(listedIds("?companyId=" + a.getId())).containsExactly(draft.getId().toString(),
                approved.getId().toString(), rejected.getId().toString(), pending.getId().toString());
        list("").andExpect(jsonPath("$.total").value(5));
    }

    // ---- chi tiết ----

    @Test
    void detailShowsAnyStatus() throws Exception {
        Job rejected = job(a, JobStatus.REJECTED, "Lập trình viên Java");

        detail(rejected.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("Thiếu mô tả công việc"))
                .andExpect(jsonPath("$.reviewedAt").value("2026-11-10T03:00:00Z"))
                .andExpect(jsonPath("$.reviewedBy.id").value(STAFF.toString()))
                .andExpect(jsonPath("$.reviewedBy.fullName").value("Hoàng Thu Hà"))
                .andExpect(jsonPath("$.companyLocked").value(false))
                .andExpect(jsonPath("$.job.id").value(rejected.getId().toString()))
                .andExpect(jsonPath("$.job.title").value("Lập trình viên Java"))
                .andExpect(jsonPath("$.job.skills[0].skillId").value(JAVA.toString()))
                .andExpect(jsonPath("$.job.company.name").value(a.getName()));

        Company locked = saveCompany("3333333333", CompanyStatus.LOCKED);
        Job draft = job(locked, JobStatus.DRAFT, "Nháp của công ty khoá");
        detail(draft.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.reviewedBy").value(nullValue()))
                .andExpect(jsonPath("$.companyLocked").value(true));
    }

    @Test
    void missingJobIs404() throws Exception {
        UUID missing = UUID.randomUUID();
        detail(missing).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOT_FOUND"));
        approve(missing).andExpect(status().isNotFound());
        reject(missing, reason("Thiếu mô tả")).andExpect(status().isNotFound());
        close(missing, reason("Vi phạm")).andExpect(status().isNotFound());
        assertThat(companyActions.count()).isZero();
    }

    @Test
    void identityDownStillReturnsDetail() throws Exception {
        Job rejected = job(a, JobStatus.REJECTED, "Lập trình viên Java");
        when(identity.lookupUsers(any())).thenThrow(new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE));

        detail(rejected.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewedBy.id").value(STAFF.toString()))
                .andExpect(jsonPath("$.reviewedBy.fullName").value(nullValue()));
    }

    // ---- duyệt ----

    @Test
    void approvePendingWritesHistoryAndPublishes() throws Exception {
        Job pending = job(a, JobStatus.PENDING, "Lập trình viên Java");

        approve(pending.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.rejectReason").value(nullValue()))
                .andExpect(jsonPath("$.reviewedBy.id").value(STAFF.toString()))
                .andExpect(jsonPath("$.reviewedBy.fullName").value("Hoàng Thu Hà"))
                .andExpect(jsonPath("$.reviewedAt").exists())
                .andExpect(jsonPath("$.job.id").value(pending.getId().toString()));

        Job reloaded = reload(pending);
        assertThat(reloaded.getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(reloaded.getReviewedBy()).isEqualTo(STAFF);
        assertThat(reloaded.getReviewedAt()).isNotNull();

        List<CompanyAction> history = companyActions.findAll();
        assertThat(history).hasSize(1);
        CompanyAction row = history.get(0);
        assertThat(row.getAction()).isEqualTo(CompanyActionType.APPROVE_JD);
        assertThat(row.getStaffId()).isEqualTo(STAFF);
        assertThat(row.getCompanyId()).isEqualTo(a.getId());
        assertThat(row.getTargetId()).isEqualTo(pending.getId());
        assertThat(row.getReason()).isNull();

        String body = mvc.perform(get("/jobs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> publicIds = JsonPath.read(body, "$.items[*].id");
        assertThat(publicIds).containsExactly(pending.getId().toString());
    }

    @Test
    void approveNonPendingIs409() throws Exception {
        for (JobStatus s : List.of(JobStatus.DRAFT, JobStatus.REJECTED, JobStatus.APPROVED, JobStatus.CLOSED)) {
            Job job = job(a, s, "JD " + s);
            approve(job.getId()).andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS"));
            assertThat(reload(job).getStatus()).isEqualTo(s);
        }
        assertThat(companyActions.count()).isZero();
    }

    @Test
    void approveExpiredPendingIs409() throws Exception {
        Job expired = job(a, JobStatus.PENDING, "Quá hạn", today().minusDays(1));

        approve(expired.getId()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_EXPIRED"))
                .andExpect(jsonPath("$.message").value("JD đã quá hạn nộp, hãy trả lại để HR sửa hạn nộp"));
        assertThat(reload(expired).getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(companyActions.count()).isZero();

        Job dueToday = job(a, JobStatus.PENDING, "Hạn hôm nay", today());
        approve(dueToday.getId()).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
    }

    // ---- trả lại ----

    @Test
    void rejectNeedsReason() throws Exception {
        Job pending = job(a, JobStatus.PENDING, "Lập trình viên Java");

        reject(pending.getId(), Map.of()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.reason").exists());
        reject(pending.getId(), reason("x".repeat(1001))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
        assertThat(reload(pending).getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(companyActions.count()).isZero();

        reject(pending.getId(), reason("  Thiếu mô tả quyền lợi  ")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectReason").value("Thiếu mô tả quyền lợi"))
                .andExpect(jsonPath("$.reviewedBy.fullName").value("Hoàng Thu Hà"));
        List<CompanyAction> history = companyActions.findAll();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(CompanyActionType.REJECT_JD);
        assertThat(history.get(0).getReason()).isEqualTo("Thiếu mô tả quyền lợi");
        assertThat(history.get(0).getTargetId()).isEqualTo(pending.getId());

        // HR thấy lý do, sửa rồi gửi lại.
        mvc.perform(get("/companies/me/jobs/" + pending.getId()).header("Authorization", hr1()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejectReason").value("Thiếu mô tả quyền lợi"));
        Map<String, Object> update = new LinkedHashMap<>();
        update.put("title", "Lập trình viên Java");
        update.put("position", "Backend");
        update.put("level", "JUNIOR");
        update.put("type", "FULLTIME");
        update.put("quantity", 2);
        update.put("city", "Hà Nội");
        update.put("workMode", "ONSITE");
        update.put("description", "Phát triển API");
        update.put("benefits", "Thưởng tháng 13");
        update.put("deadline", today().plusDays(10).toString());
        mvc.perform(put("/jobs/" + pending.getId()).header("Authorization", hr1())
                        .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(update)))
                .andExpect(status().isOk());
        mvc.perform(post("/jobs/" + pending.getId() + "/submit").header("Authorization", hr1()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void blankReasonRejected() throws Exception {
        Job pending = job(a, JobStatus.PENDING, "Chờ duyệt");
        Job approved = job(a, JobStatus.APPROVED, "Đã duyệt");

        reject(pending.getId(), reason("   ")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.reason").exists());
        close(approved.getId(), reason("   ")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.reason").exists());

        assertThat(reload(pending).getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(reload(pending).getRejectReason()).isNull();
        assertThat(reload(approved).getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(companyActions.count()).isZero();
    }

    @Test
    void rejectNonPendingIs409() throws Exception {
        Job approved = job(a, JobStatus.APPROVED, "Đã duyệt");
        reject(approved.getId(), reason("Thiếu mô tả")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS"));
        assertThat(reload(approved).getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(companyActions.count()).isZero();
    }

    // ---- đóng ----

    @Test
    void closeApprovedOnly() throws Exception {
        Job approved = job(a, JobStatus.APPROVED, "Đã duyệt");

        close(approved.getId(), reason(" Vi phạm quy định đăng tin ")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.reviewedBy.id").value(STAFF.toString()));
        List<CompanyAction> history = companyActions.findAll();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(CompanyActionType.CLOSE_JD);
        assertThat(history.get(0).getReason()).isEqualTo("Vi phạm quy định đăng tin");
        assertThat(history.get(0).getTargetId()).isEqualTo(approved.getId());
        assertThat(reload(approved).getStatus()).isEqualTo(JobStatus.CLOSED);

        Job pending = job(a, JobStatus.PENDING, "Chờ duyệt");
        close(pending.getId(), reason("Vi phạm")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_JOB_STATUS"));
        assertThat(reload(pending).getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(companyActions.count()).isEqualTo(1);
    }

    // ---- phân quyền ----

    @Test
    void rolesForbidden() throws Exception {
        Job pending = job(a, JobStatus.PENDING, "Chờ duyệt");
        Job approved = job(a, JobStatus.APPROVED, "Đã duyệt");
        for (String auth : List.of(hr1(), "Bearer " + token(Role.CANDIDATE), "Bearer " + token(Role.ADMIN))) {
            mvc.perform(get("/staff/jobs?status=PENDING").header("Authorization", auth))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/staff/jobs/" + pending.getId()).header("Authorization", auth))
                    .andExpect(status().isForbidden());
            mvc.perform(post("/staff/jobs/" + pending.getId() + "/approve").header("Authorization", auth))
                    .andExpect(status().isForbidden());
            withReason("reject", pending.getId(), auth, reason("Thiếu mô tả")).andExpect(status().isForbidden());
            withReason("close", approved.getId(), auth, reason("Vi phạm")).andExpect(status().isForbidden());
        }
        mvc.perform(get("/staff/jobs")).andExpect(status().isUnauthorized());
        assertThat(reload(pending).getStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(reload(approved).getStatus()).isEqualTo(JobStatus.APPROVED);
        assertThat(companyActions.count()).isZero();
        verify(identity, never()).lookupUsers(any());
    }
}
