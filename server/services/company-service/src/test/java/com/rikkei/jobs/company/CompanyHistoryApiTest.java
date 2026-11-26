package com.rikkei.jobs.company;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyAction;
import com.rikkei.jobs.company.entity.CompanyActionType;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/** DN-07 §4.6: lịch sử thao tác của cán bộ trên công ty. */
class CompanyHistoryApiTest extends CompanyApiTestSupport {

    private static final UUID STAFF = UUID.fromString("00000000-0000-0000-0002-000000000001");

    @Autowired JdbcTemplate jdbc;

    private CompanyAction save(UUID companyId, CompanyActionType type, UUID target, String reason, String at) {
        CompanyAction a = companyActions.save(new CompanyAction(companyId, type, target, reason, STAFF));
        jdbc.update("update company.company_actions set created_at = ? where id = ?",
                Timestamp.from(Instant.parse(at)), a.getId());
        return a;
    }

    private String staff() {
        return "Bearer " + token(STAFF, Role.STAFF);
    }

    @Test
    void newestFirstWithStaffNameJobTitleAndReason() throws Exception {
        Company c = saveCompany("0700000001", CompanyStatus.ACTIVE);
        Job job = saveJob(c.getId(), JobStatus.APPROVED);
        save(c.getId(), CompanyActionType.LOCK, null, "Vi phạm", "2026-11-01T01:00:00Z");
        save(c.getId(), CompanyActionType.APPROVE_JD, job.getId(), null, "2026-11-02T01:00:00Z");
        save(c.getId(), CompanyActionType.REJECT_JD, job.getId(), "Thiếu mô tả", "2026-11-03T01:00:00Z");
        when(identity.lookupUsers(anyList())).thenReturn(List.of(
                new IdentityUserStatus(STAFF, "ha@rikkei.vn", "Hoàng Thu Hà", "ACTIVE", null, null, null, false)));

        mvc.perform(get("/staff/companies/" + c.getId() + "/history").header("Authorization", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items[0].action").value("REJECT_JD"))
                .andExpect(jsonPath("$.items[0].reason").value("Thiếu mô tả"))
                .andExpect(jsonPath("$.items[0].job.id").value(job.getId().toString()))
                .andExpect(jsonPath("$.items[0].job.title").value("JD mẫu"))
                .andExpect(jsonPath("$.items[0].staff.fullName").value("Hoàng Thu Hà"))
                .andExpect(jsonPath("$.items[0].staff.id").value(STAFF.toString()))
                .andExpect(jsonPath("$.items[1].action").value("APPROVE_JD"))
                .andExpect(jsonPath("$.items[1].reason").value(nullValue()))
                .andExpect(jsonPath("$.items[2].action").value("LOCK"))
                .andExpect(jsonPath("$.items[2].job").value(nullValue()));
    }

    @Test
    void paged() throws Exception {
        Company c = saveCompany("0700000002", CompanyStatus.ACTIVE);
        for (int i = 0; i < 25; i++) {
            save(c.getId(), CompanyActionType.LOCK, null, "r" + i, "2026-11-01T00:00:00Z");
        }
        when(identity.lookupUsers(anyList())).thenReturn(List.of());

        mvc.perform(get("/staff/companies/" + c.getId() + "/history?page=2&size=20").header("Authorization", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(25))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.items", hasSize(5)));
    }

    @Test
    void identityDownKeepsHistory() throws Exception {
        Company c = saveCompany("0700000003", CompanyStatus.ACTIVE);
        save(c.getId(), CompanyActionType.LOCK, null, "x", "2026-11-01T00:00:00Z");
        when(identity.lookupUsers(anyList()))
                .thenThrow(new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE, "down"));

        mvc.perform(get("/staff/companies/" + c.getId() + "/history").header("Authorization", staff()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].staff.id").value(STAFF.toString()))
                .andExpect(jsonPath("$.items[0].staff.fullName").value(nullValue()));
    }

    @Test
    void unknownCompanyIs404() throws Exception {
        mvc.perform(get("/staff/companies/" + UUID.randomUUID() + "/history").header("Authorization", staff()))
                .andExpect(status().isNotFound());
    }

    @Test
    void rolesForbidden() throws Exception {
        Company c = saveCompany("0700000004", CompanyStatus.ACTIVE);
        mvc.perform(get("/staff/companies/" + c.getId() + "/history")
                        .header("Authorization", "Bearer " + token(Role.HR)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/staff/companies/" + c.getId() + "/history")
                        .header("Authorization", "Bearer " + token(Role.CANDIDATE)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/staff/companies/" + c.getId() + "/history")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPagingIs400() throws Exception {
        Company c = saveCompany("0700000005", CompanyStatus.ACTIVE);
        mvc.perform(get("/staff/companies/" + c.getId() + "/history?page=0").header("Authorization", staff()))
                .andExpect(status().isBadRequest());
    }
}
