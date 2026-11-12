package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.rikkei.jobs.recruitment.entity.ApplicationNotificationOutbox;
import com.rikkei.jobs.recruitment.repository.ApplicationNotificationOutboxRepository;
import com.rikkei.jobs.recruitment.service.CvSnapshotStorage;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.DataIntegrityViolationException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplyApplicationFailureTest extends ApplicationIntegrationSupport {
    @SpyBean CvSnapshotStorage storage;
    @SpyBean ApplicationNotificationOutboxRepository outbox;

    @Test void snapshotFailureDoesNotCreateAnyDatabaseRows() throws Exception {
        doThrow(new RecruitmentDependencyException(503, "SNAPSHOT_UNAVAILABLE", "Storage unavailable"))
            .when(storage).store(any(), any());
        mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate()).contentType("application/json").content(body()))
            .andExpect(status().isServiceUnavailable());
        assertThat(count("applications")).isZero();
        assertThat(count("application_status_history")).isZero();
        assertThat(count("application_notification_outbox")).isZero();
    }

    @Test void failureAfterApplicationInsertRollsBackHistoryAndOutboxAndDeletesSnapshot() throws Exception {
        doThrow(new DataIntegrityViolationException("Simulated outbox write failure"))
            .when(outbox).save(any(ApplicationNotificationOutbox.class));
        mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate()).contentType("application/json").content(body()))
            .andExpect(status().isConflict());
        assertThat(count("applications")).isZero();
        assertThat(count("application_status_history")).isZero();
        assertThat(count("application_notification_outbox")).isZero();
        var id = ArgumentCaptor.forClass(UUID.class);
        verify(storage).store(id.capture(), any());
        verify(storage).delete(id.getValue());
        assertThatThrownBy(() -> storage.read(id.getValue())).isInstanceOf(RecruitmentDependencyException.class);
    }

    @Test void deadlineIsCheckedAgainInsideTheWriteTransaction() throws Exception {
        when(clock.instant()).thenReturn(NOW, NOW, NOW.plusSeconds(7200));
        mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate()).contentType("application/json").content(body()))
            .andExpect(status().isConflict());
        assertThat(count("applications")).isZero();
        verify(storage).delete(any());
    }
}
