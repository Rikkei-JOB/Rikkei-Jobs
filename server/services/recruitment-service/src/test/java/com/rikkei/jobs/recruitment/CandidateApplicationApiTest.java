package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.exception.ForbiddenException;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.dto.integration.CvPdf;
import com.rikkei.jobs.recruitment.service.CvSnapshotStorage;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CandidateApplicationApiTest extends ApplicationIntegrationSupport {
    @Autowired CvSnapshotStorage snapshots;

    @Test void submissionIsAtomicAndDuplicateIs409() throws Exception {
        var id = apply();
        assertThat(count("applications")).isEqualTo(1);
        assertThat(count("application_status_history")).isEqualTo(1);
        assertThat(count("application_notification_outbox")).isEqualTo(1);
        mvc.perform(get("/recruitment/me/applications/" + id).header("Authorization", candidate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.canWithdraw").value(true)).andExpect(jsonPath("$.history[0].fromStatus").isEmpty())
            .andExpect(jsonPath("$.history[0].toStatus").value("SUBMITTED"));
        mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate()).contentType("application/json").content(body()))
            .andExpect(status().isConflict());
        assertThat(count("applications")).isEqualTo(1);
        assertThat(count("application_status_history")).isEqualTo(1);
    }

    @Test void withdrawalAndReapplicationKeepOldHistoryAndSnapshot() throws Exception {
        var old = apply();
        byte[] oldBytes = snapshots.read(old);
        mvc.perform(post("/recruitment/me/applications/" + old + "/withdraw").header("Authorization", candidate()).contentType("application/json").content("{\"reason\":\"  Thay đổi kế hoạch  \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WITHDRAWN"))
            .andExpect(jsonPath("$.history[1].note").value("Thay đổi kế hoạch"));
        when(cvs.exportOwnedPdf(eq(cv), anyString())).thenReturn(new CvPdf("%PDF-1.7\nnew-cv".getBytes()));
        var newer = apply();
        assertThat(newer).isNotEqualTo(old);
        assertThat(snapshots.read(old)).isEqualTo(oldBytes);
        assertThat(snapshots.read(newer)).isNotEqualTo(oldBytes);
        assertThat(count("application_status_history")).isEqualTo(3);
    }

    @Test void rejectedAllowsNewApplicationButASecondOpenApplicationDoesNot() throws Exception {
        var old = apply();
        jdbc.update("update recruitment.applications set status='REJECTED' where id=?", old);
        var newer = apply();
        assertThat(newer).isNotEqualTo(old);
        assertThat(jdbc.queryForObject("select status from recruitment.applications where id=?", String.class, old)).isEqualTo("REJECTED");
        mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate()).contentType("application/json").content(body())).andExpect(status().isConflict());
        assertThat(count("applications")).isEqualTo(2);
    }

    @Test void pendingExpiredExactDeadlineAndLockedCompanyAreBlocked() throws Exception {
        for (var context : java.util.List.of(context("PENDING", NOW.plusSeconds(100), false), context("APPROVED", NOW.minusSeconds(1), false), context("APPROVED", NOW, false), context("APPROVED", NOW.plusSeconds(100), true))) {
            when(jobs.getContext(job)).thenReturn(context);
            mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate()).contentType("application/json").content(body())).andExpect(status().isConflict());
        }
        assertThat(count("applications")).isZero();
    }

    @Test void expirationDuringPdfRenderingDoesNotCreateAnApplication() throws Exception {
        when(cvs.exportOwnedPdf(eq(cv), anyString())).thenAnswer(call -> {
            when(clock.instant()).thenReturn(NOW.plusSeconds(7200));
            return new CvPdf("%PDF-1.7\ncv".getBytes());
        });
        mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate()).contentType("application/json").content(body())).andExpect(status().isConflict());
        assertThat(count("applications")).isZero();
        assertThat(count("application_notification_outbox")).isZero();
    }

    @Test void wrongCvOwnerDoesNotCreateApplicationOrOutbox() throws Exception {
        when(cvs.exportOwnedPdf(eq(cv), anyString())).thenThrow(new ForbiddenException("CV không thuộc ứng viên."));
        mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate()).contentType("application/json").content(body())).andExpect(status().isForbidden());
        assertThat(count("applications")).isZero();
        assertThat(count("application_status_history")).isZero();
        assertThat(count("application_notification_outbox")).isZero();
    }

    @Test void offerCannotBeWithdrawnAndDoesNotAddHistory() throws Exception {
        var id = apply();
        jdbc.update("update recruitment.applications set status='OFFER' where id=?", id);
        mvc.perform(post("/recruitment/me/applications/" + id + "/withdraw").header("Authorization", candidate()).contentType("application/json").content("{\"reason\":\"Không tiếp tục\"}"))
            .andExpect(status().isConflict());
        assertThat(count("application_status_history")).isEqualTo(1);
    }

    @Test void ownershipRoleAndPdfPrivacyAreEnforced() throws Exception {
        var id = apply();
        var other = token(UUID.randomUUID(), Role.CANDIDATE, null);
        mvc.perform(get("/recruitment/me/applications/" + id).header("Authorization", other)).andExpect(status().isForbidden());
        mvc.perform(get("/recruitment/applications/" + id + "/cv").header("Authorization", other)).andExpect(status().isForbidden());
        mvc.perform(get("/recruitment/applications/" + id + "/cv").header("Authorization", token(UUID.randomUUID(), Role.HR, UUID.randomUUID()))).andExpect(status().isForbidden());
        mvc.perform(get("/recruitment/applications/" + id + "/cv").header("Authorization", token(UUID.randomUUID(), Role.HR, company)))
            .andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andExpect(header().string("Cache-Control", "private, no-store"))
            .andExpect(content().bytes(snapshots.read(id)));
        mvc.perform(get("/recruitment/me/applications")).andExpect(status().isUnauthorized());
        mvc.perform(get("/recruitment/me/applications").header("Authorization", token(UUID.randomUUID(), Role.HR, company))).andExpect(status().isForbidden());
    }

    @Test void paginationFilterAndHistoryNotesDoNotLeakData() throws Exception {
        var id = apply();
        jdbc.update("insert into recruitment.application_status_history(id,application_id,from_status,to_status,actor_id,note,created_at) values (?,?, 'SUBMITTED','SCREENING',?, 'private HR note',?)", UUID.randomUUID(), id, UUID.randomUUID(), java.sql.Timestamp.from(NOW.plusSeconds(1)));
        mvc.perform(get("/recruitment/me/applications/" + id).header("Authorization", candidate())).andExpect(status().isOk()).andExpect(jsonPath("$.history[1].note").isEmpty());
        mvc.perform(get("/recruitment/me/applications?status=SUBMITTED").header("Authorization", candidate())).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/recruitment/me/applications?status=REJECTED").header("Authorization", candidate())).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/recruitment/me/applications?page=2").header("Authorization", candidate())).andExpect(jsonPath("$.items").isEmpty()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/recruitment/me/applications").header("Authorization", token(UUID.randomUUID(), Role.CANDIDATE, null))).andExpect(jsonPath("$.total").value(0));
        for (String query : java.util.List.of("page=0", "size=101", "status=UNKNOWN")) {
            mvc.perform(get("/recruitment/me/applications?" + query).header("Authorization", candidate())).andExpect(status().isBadRequest());
        }
    }

    @Test void blankWithdrawalReasonAndMissingCvAreRejected() throws Exception {
        var id = apply();
        mvc.perform(post("/recruitment/me/applications/" + id + "/withdraw").header("Authorization", candidate()).contentType("application/json").content("{\"reason\":\"   \"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate()).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
    }
}
