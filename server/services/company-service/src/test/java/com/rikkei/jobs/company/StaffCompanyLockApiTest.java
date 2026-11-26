package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** DN-07 §4.5: cán bộ khoá / mở khoá công ty; HR của công ty bị khoá mất quyền truy cập. */
class StaffCompanyLockApiTest extends CompanyApiTestSupport {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final UUID STAFF = UUID.fromString("00000000-0000-0000-0002-000000000001");
    private static final UUID HR_NEW = UUID.fromString("00000000-0000-0000-0001-000000000003");
    private static final String STAFF_NAME = "Hoàng Thu Hà";
    private static final String LOCK_REASON = "Đăng tin sai sự thật";

    private Company a;
    private Company b;

    @BeforeEach
    void setUp() {
        a = saveCompany("1111111111", CompanyStatus.ACTIVE);
        b = saveCompany("2222222222", CompanyStatus.ACTIVE);
        linkHr(HR1, a.getId());
        linkHr(HR_NEW, a.getId());
        linkHr(HR2, b.getId());
        when(identity.setCompanyLock(anyList(), anyBoolean())).thenAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            boolean locked = inv.getArgument(1);
            return ids.stream().map(id -> hrStatus(id, locked)).toList();
        });
        when(identity.lookupUsers(anyList())).thenAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            return ids.stream().map(id -> STAFF.equals(id)
                    ? new IdentityUserStatus(STAFF, "ha.ht@rikkei.vn", STAFF_NAME, "ACTIVE", null, null, null, false)
                    : hrStatus(id, false)).toList();
        });
    }

    // ---- tiện ích ----

    private static IdentityUserStatus hrStatus(UUID id, boolean companyLocked) {
        return new IdentityUserStatus(id, id + "@hr.test", "HR " + id, companyLocked ? "LOCKED" : "ACTIVE",
                null, null, null, companyLocked);
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private String staff() {
        return "Bearer " + token(STAFF, Role.STAFF);
    }

    private Job job(Company company, JobStatus status, String title) {
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
        job.setDeadline(today().plusDays(10));
        job.setStatus(status);
        job.setCreatedBy(UUID.randomUUID());
        return jobs.save(job);
    }

    private static Map<String, Object> reason(String reason) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reason", reason);
        return m;
    }

    private ResultActions lock(UUID companyId, Object body) throws Exception {
        return lock(companyId, body, staff());
    }

    private ResultActions lock(UUID companyId, Object body, String auth) throws Exception {
        return mvc.perform(post("/staff/companies/" + companyId + "/lock").header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(JSON.writeValueAsString(body)));
    }

    private ResultActions unlock(UUID companyId) throws Exception {
        return unlock(companyId, staff());
    }

    private ResultActions unlock(UUID companyId, String auth) throws Exception {
        return mvc.perform(post("/staff/companies/" + companyId + "/unlock").header("Authorization", auth));
    }

    private List<String> publicJobIds() throws Exception {
        String body = mvc.perform(get("/jobs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.items[*].id");
    }

    private CompanyStatus statusOf(Company company) {
        return companies.findById(company.getId()).orElseThrow().getStatus();
    }

    @SuppressWarnings("unchecked")
    private List<List<UUID>> setCompanyLockCalls(boolean locked, int times) {
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(identity, times(times)).setCompanyLock(captor.capture(), eq(locked));
        return captor.getAllValues();
    }

    // ---- khoá / mở khoá ----

    @Test
    void lockCompanyHidesJobsAndBlocksHr() throws Exception {
        Job ofA = job(a, JobStatus.APPROVED, "JD của A");
        Job ofB = job(b, JobStatus.APPROVED, "JD của B");
        assertThat(publicJobIds()).containsExactlyInAnyOrder(ofA.getId().toString(), ofB.getId().toString());

        lock(a.getId(), reason(LOCK_REASON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.getId().toString()))
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.lock.reason").value(LOCK_REASON))
                .andExpect(jsonPath("$.lock.lockedAt").isNotEmpty())
                .andExpect(jsonPath("$.lock.lockedBy.id").value(STAFF.toString()))
                .andExpect(jsonPath("$.lock.lockedBy.fullName").value(STAFF_NAME))
                .andExpect(jsonPath("$.hrAccounts.length()").value(2))
                .andExpect(jsonPath("$.hrAccounts[0].companyLocked").value(true))
                .andExpect(jsonPath("$.hrAccounts[1].companyLocked").value(true));

        List<List<UUID>> calls = setCompanyLockCalls(true, 1);
        assertThat(calls.get(0)).containsExactlyInAnyOrder(HR1, HR_NEW);
        verify(identity, never()).setCompanyLock(anyList(), eq(false));
        assertThat(statusOf(a)).isEqualTo(CompanyStatus.LOCKED);

        List<CompanyAction> history = companyActions.findAll();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getAction()).isEqualTo(CompanyActionType.LOCK);
        assertThat(history.get(0).getReason()).isEqualTo(LOCK_REASON);
        assertThat(history.get(0).getStaffId()).isEqualTo(STAFF);
        assertThat(history.get(0).getCompanyId()).isEqualTo(a.getId());
        assertThat(history.get(0).getTargetId()).isNull();

        assertThat(publicJobIds()).containsExactly(ofB.getId().toString());
        mvc.perform(get("/jobs/" + ofA.getId())).andExpect(status().isNotFound());
        mvc.perform(get("/companies/me").header("Authorization", "Bearer " + token(HR1, Role.HR)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"))
                .andExpect(jsonPath("$.message").value("Công ty của bạn đang bị khoá, liên hệ cán bộ tuyển dụng"));
        mvc.perform(get("/companies/me").header("Authorization", "Bearer " + token(HR2, Role.HR)))
                .andExpect(status().isOk());

        // GET /staff/companies/{id} trả cùng thông tin khoá cho banner.
        mvc.perform(get("/staff/companies/" + a.getId()).header("Authorization", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lock.reason").value(LOCK_REASON))
                .andExpect(jsonPath("$.lock.lockedBy.fullName").value(STAFF_NAME));
        mvc.perform(get("/staff/companies/" + b.getId()).header("Authorization", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lock").value(nullValue()));
    }

    @Test
    void unlockRestores() throws Exception {
        Job ofA = job(a, JobStatus.APPROVED, "JD của A");
        lock(a.getId(), reason(LOCK_REASON)).andExpect(status().isOk());

        unlock(a.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.lock").value(nullValue()))
                .andExpect(jsonPath("$.hrAccounts[0].companyLocked").value(false));

        assertThat(setCompanyLockCalls(false, 1).get(0)).containsExactlyInAnyOrder(HR1, HR_NEW);
        assertThat(statusOf(a)).isEqualTo(CompanyStatus.ACTIVE);
        List<CompanyAction> unlocks = companyActions.findAll().stream()
                .filter(r -> r.getAction() == CompanyActionType.UNLOCK).toList();
        assertThat(unlocks).hasSize(1);
        assertThat(unlocks.get(0).getReason()).isNull();
        assertThat(unlocks.get(0).getStaffId()).isEqualTo(STAFF);
        assertThat(publicJobIds()).containsExactly(ofA.getId().toString());
        mvc.perform(get("/companies/me").header("Authorization", "Bearer " + token(HR1, Role.HR)))
                .andExpect(status().isOk());
    }

    @Test
    void lockTwiceOrUnlockActiveIs409() throws Exception {
        unlock(a.getId()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Công ty không bị khoá"));
        lock(a.getId(), reason(LOCK_REASON)).andExpect(status().isOk());
        lock(a.getId(), reason("Lần hai")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Công ty đã bị khoá"));

        assertThat(companyActions.count()).isEqualTo(1);
        setCompanyLockCalls(true, 1);
        verify(identity, never()).setCompanyLock(anyList(), eq(false));

        lock(UUID.randomUUID(), reason(LOCK_REASON)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        unlock(UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    void blankLockReasonRejected() throws Exception {
        for (Object body : List.of(Map.of(), reason("   "), reason(" \t"), reason(" "),
                reason("     "), reason("x".repeat(1001)))) {
            lock(a.getId(), body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.errors.reason").exists());
        }
        assertThat(statusOf(a)).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(companyActions.count()).isZero();
        verify(identity, never()).setCompanyLock(anyList(), anyBoolean());
    }

    @Test
    void paddedReasonIsTrimmedBeforeLengthCheck() throws Exception {
        String reason = "x".repeat(1000);
        lock(a.getId(), reason("  " + reason + "  ")).andExpect(status().isOk())
                .andExpect(jsonPath("$.lock.reason").value(reason));
        assertThat(companyActions.findAll().get(0).getReason()).isEqualTo(reason);
    }

    @Test
    void lockCompanyWithoutHrSkipsIdentity() throws Exception {
        Company c = saveCompany("3333333333", CompanyStatus.ACTIVE);

        lock(c.getId(), reason(LOCK_REASON)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"))
                .andExpect(jsonPath("$.hrAccounts").isEmpty());
        unlock(c.getId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(identity, never()).setCompanyLock(any(), anyBoolean());
        assertThat(companyActions.count()).isEqualTo(2);
    }

    @Test
    void identityDownLeavesCompanyUntouched() throws Exception {
        when(identity.setCompanyLock(anyList(), anyBoolean())).thenThrow(new CompanyException(
                CompanyErrorCode.EXTERNAL_UNAVAILABLE, "Không cập nhật được tài khoản HR, vui lòng thử lại."));

        lock(a.getId(), reason(LOCK_REASON)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("Không cập nhật được tài khoản HR, vui lòng thử lại."));

        assertThat(statusOf(a)).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(companyActions.count()).isZero();
        // Lô lỗi vẫn được gọi bù (có thể identity đã áp dụng trước khi timeout); gọi bù lỗi chỉ ghi log.
        List<UUID> sent = setCompanyLockCalls(true, 1).get(0);
        assertThat(sent).containsExactlyInAnyOrder(HR1, HR_NEW);
        assertThat(setCompanyLockCalls(false, 1).get(0)).containsExactlyElementsOf(sent);

        Company locked = saveCompany("3333333333", CompanyStatus.LOCKED);
        linkHr(UUID.randomUUID(), locked.getId());
        unlock(locked.getId()).andExpect(status().isServiceUnavailable());
        assertThat(statusOf(locked)).isEqualTo(CompanyStatus.LOCKED);
        assertThat(companyActions.count()).isZero();
    }

    @Test
    void timedOutBatchIsCompensated() throws Exception {
        // identity áp dụng lô rồi mới timeout: cờ phải được đặt lại dù lời gọi báo lỗi.
        when(identity.setCompanyLock(anyList(), eq(true))).thenThrow(new CompanyException(
                CompanyErrorCode.EXTERNAL_UNAVAILABLE, "Không cập nhật được tài khoản HR, vui lòng thử lại."));

        lock(a.getId(), reason(LOCK_REASON)).andExpect(status().isServiceUnavailable());

        List<UUID> sent = setCompanyLockCalls(true, 1).get(0);
        assertThat(setCompanyLockCalls(false, 1).get(0)).containsExactlyElementsOf(sent);
        assertThat(statusOf(a)).isEqualTo(CompanyStatus.ACTIVE);
    }

    @Test
    void partialBatchFailureOnUnlockCompensatesWithTrue() throws Exception {
        Company big = saveCompany("4444444444", CompanyStatus.LOCKED);
        for (int i = 0; i < 150; i++) {
            linkHr(UUID.randomUUID(), big.getId());
        }
        when(identity.setCompanyLock(anyList(), anyBoolean())).thenAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            boolean locked = inv.getArgument(1);
            if (!locked && ids.size() == 50) {
                throw new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE,
                        "Không cập nhật được tài khoản HR, vui lòng thử lại.");
            }
            return ids.stream().map(id -> hrStatus(id, locked)).toList();
        });

        unlock(big.getId()).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_UNAVAILABLE"));

        List<List<UUID>> sent = setCompanyLockCalls(false, 2);
        assertThat(sent).extracting(List::size).containsExactly(100, 50);
        List<List<UUID>> compensated = setCompanyLockCalls(true, 2);
        assertThat(compensated.get(0)).containsExactlyElementsOf(sent.get(0));
        assertThat(compensated.get(1)).containsExactlyElementsOf(sent.get(1));
        assertThat(statusOf(big)).isEqualTo(CompanyStatus.LOCKED);
        assertThat(companyActions.count()).isZero();
    }

    @Test
    void individuallyLockedHrStaysLockedAfterCompanyUnlock() throws Exception {
        Instant lockedAt = Instant.parse("2026-11-17T03:00:00Z");
        CompanyHr hr = companyHrs.findById(HR_NEW).orElseThrow();
        hr.setLockedAt(lockedAt);
        companyHrs.save(hr);

        lock(a.getId(), reason(LOCK_REASON)).andExpect(status().isOk());
        unlock(a.getId()).andExpect(status().isOk());

        assertThat(companyHrs.findById(HR_NEW).orElseThrow().getLockedAt()).isEqualTo(lockedAt);
        mvc.perform(get("/companies/me").header("Authorization", "Bearer " + token(HR_NEW, Role.HR)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Tài khoản đã bị khoá"));
        mvc.perform(get("/companies/me").header("Authorization", "Bearer " + token(HR1, Role.HR)))
                .andExpect(status().isOk());
    }

    @Test
    void partialBatchFailureCompensates() throws Exception {
        Company big = saveCompany("4444444444", CompanyStatus.ACTIVE);
        for (int i = 0; i < 150; i++) {
            linkHr(UUID.randomUUID(), big.getId());
        }
        when(identity.setCompanyLock(anyList(), anyBoolean())).thenAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            boolean locked = inv.getArgument(1);
            assertThat(ids.size()).isLessThanOrEqualTo(100);
            if (locked && ids.size() == 50) {
                throw new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE,
                        "Không cập nhật được tài khoản HR, vui lòng thử lại.");
            }
            return ids.stream().map(id -> hrStatus(id, locked)).toList();
        });

        lock(big.getId(), reason(LOCK_REASON)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("EXTERNAL_UNAVAILABLE"));

        List<List<UUID>> sent = setCompanyLockCalls(true, 2);
        assertThat(sent).extracting(List::size).containsExactly(100, 50);
        // Gọi bù cả lô đã thành công lẫn lô lỗi.
        List<List<UUID>> compensated = setCompanyLockCalls(false, 2);
        assertThat(compensated.get(0)).containsExactlyElementsOf(sent.get(0));
        assertThat(compensated.get(1)).containsExactlyElementsOf(sent.get(1));
        assertThat(statusOf(big)).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(companyActions.count()).isZero();
    }

    @Test
    void rollbackAfterIdentityCompensates() throws Exception {
        // identity nhận xong thì commit DB lỗi: synchronization ném ở beforeCommit làm transaction rollback.
        when(identity.setCompanyLock(anyList(), eq(true))).thenAnswer(inv -> {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    throw new IllegalStateException("commit lỗi giả lập");
                }
            });
            List<UUID> ids = inv.getArgument(0);
            return ids.stream().map(id -> hrStatus(id, true)).toList();
        });

        lock(a.getId(), reason(LOCK_REASON)).andExpect(status().isInternalServerError());

        List<UUID> sent = setCompanyLockCalls(true, 1).get(0);
        assertThat(sent).containsExactlyInAnyOrder(HR1, HR_NEW);
        assertThat(setCompanyLockCalls(false, 1).get(0)).containsExactlyInAnyOrderElementsOf(sent);
        assertThat(statusOf(a)).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(companyActions.count()).isZero();
    }

    @Test
    void approvedJobOfLockedCompanyStaysHiddenUntilUnlock() throws Exception {
        Job pending = job(a, JobStatus.PENDING, "Chờ duyệt");
        lock(a.getId(), reason(LOCK_REASON)).andExpect(status().isOk());

        mvc.perform(post("/staff/jobs/" + pending.getId() + "/approve").header("Authorization", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.companyLocked").value(true));
        assertThat(publicJobIds()).doesNotContain(pending.getId().toString());

        unlock(a.getId()).andExpect(status().isOk());
        assertThat(publicJobIds()).containsExactly(pending.getId().toString());
    }

    @Test
    void staffHrAccountShowsCompanyLock() throws Exception {
        lock(a.getId(), reason(LOCK_REASON)).andExpect(status().isOk());
        when(identity.lookupUsers(anyList())).thenAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            return ids.stream().map(id -> STAFF.equals(id)
                    ? new IdentityUserStatus(STAFF, "ha.ht@rikkei.vn", STAFF_NAME, "ACTIVE", null, null, null, false)
                    : hrStatus(id, true)).toList();
        });

        mvc.perform(get("/staff/companies/" + a.getId()).header("Authorization", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hrAccounts.length()").value(2))
                .andExpect(jsonPath("$.hrAccounts[0].companyLocked").value(true))
                .andExpect(jsonPath("$.hrAccounts[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.hrAccounts[1].companyLocked").value(true))
                .andExpect(jsonPath("$.hrAccounts[1].status").value("ACTIVE"));
    }

    @Test
    void lockInfoNameNullWhenIdentityDown() throws Exception {
        lock(a.getId(), reason(LOCK_REASON)).andExpect(status().isOk());
        Company c = saveCompany("3333333333", CompanyStatus.ACTIVE);
        lock(c.getId(), reason(LOCK_REASON)).andExpect(status().isOk());
        when(identity.lookupUsers(anyList())).thenThrow(new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE));

        // Công ty không có HR: chi tiết vẫn 200, chỉ thiếu tên cán bộ khoá.
        mvc.perform(get("/staff/companies/" + c.getId()).header("Authorization", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lock.reason").value(LOCK_REASON))
                .andExpect(jsonPath("$.lock.lockedBy.id").value(STAFF.toString()))
                .andExpect(jsonPath("$.lock.lockedBy.fullName").value(nullValue()));
    }

    @Test
    void concurrentLockSerialized() throws Exception {
        // Lời gọi identity chậm giữ khoá dòng đủ lâu để lời gọi thứ hai phải chờ.
        when(identity.setCompanyLock(anyList(), anyBoolean())).thenAnswer(inv -> {
            Thread.sleep(300);
            List<UUID> ids = inv.getArgument(0);
            boolean locked = inv.getArgument(1);
            return ids.stream().map(id -> hrStatus(id, locked)).toList();
        });
        String auth = staff();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> call = () -> {
            start.await();
            return lock(a.getId(), reason(LOCK_REASON), auth).andReturn().getResponse().getStatus();
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            futures.add(pool.submit(call));
            futures.add(pool.submit(call));
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : futures) {
                statuses.add(f.get(30, TimeUnit.SECONDS));
            }
            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        } finally {
            pool.shutdownNow();
        }
        assertThat(companyActions.count()).isEqualTo(1);
        setCompanyLockCalls(true, 1);
    }

    @Test
    void rolesForbidden() throws Exception {
        Company locked = saveCompany("3333333333", CompanyStatus.LOCKED);
        for (String auth : List.of("Bearer " + token(HR2, Role.HR), "Bearer " + token(Role.CANDIDATE),
                "Bearer " + token(Role.ADMIN))) {
            lock(a.getId(), reason(LOCK_REASON), auth).andExpect(status().isForbidden());
            unlock(locked.getId(), auth).andExpect(status().isForbidden());
        }
        mvc.perform(post("/staff/companies/" + a.getId() + "/unlock")).andExpect(status().isUnauthorized());
        assertThat(statusOf(a)).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(statusOf(locked)).isEqualTo(CompanyStatus.LOCKED);
        assertThat(companyActions.count()).isZero();
        verify(identity, never()).setCompanyLock(any(), anyBoolean());
    }
}
