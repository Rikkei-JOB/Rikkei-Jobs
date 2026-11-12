package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.dto.integration.JobApplicationContext;
import com.rikkei.jobs.recruitment.service.*;
import com.rikkei.jobs.recruitment.service.impl.ApplicationWriteServiceImpl;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.TransactionSystemException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApplicationCommitAmbiguityTest extends ApplicationIntegrationSupport {
    @MockBean ApplicationWriteService writer;
    @Autowired AutowireCapableBeanFactory factory;
    @Autowired CvSnapshotStorage snapshots;

    @Test void exceptionAfterSuccessfulCommitMustNotDeleteTheCommittedPdf() throws Exception {
        // A separately created transactional proxy commits for real before the simulated lost acknowledgement.
        ApplicationWriteService real = factory.createBean(ApplicationWriteServiceImpl.class);
        doAnswer(call -> {
            real.createSubmitted(call.getArgument(0, UUID.class), call.getArgument(1, UUID.class), call.getArgument(2, UUID.class),
                call.getArgument(3, JobApplicationContext.class), call.getArgument(4, String.class));
            throw new TransactionSystemException("Commit acknowledgement was lost");
        }).when(writer).createSubmitted(any(), any(), any(), any(), any());

        mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate()).contentType("application/json").content(body()))
            .andExpect(status().isInternalServerError());
        assertThat(count("applications")).isEqualTo(1);
        assertThat(count("application_status_history")).isEqualTo(1);
        assertThat(count("application_notification_outbox")).isEqualTo(1);
        var id = jdbc.queryForObject("select id from recruitment.applications", UUID.class);
        assertThat(snapshots.read(id)).startsWith("%PDF-".getBytes());
    }

    @Test void unknownOutcomePreservesUnreferencedFileForReconciliation() throws Exception {
        doThrow(new TransactionSystemException("Unknown transaction outcome"))
            .when(writer).createSubmitted(any(), any(), any(), any(), any());
        mvc.perform(post("/recruitment/jobs/" + job + "/apply").header("Authorization", candidate()).contentType("application/json").content(body()))
            .andExpect(status().isInternalServerError());
        var id = org.mockito.ArgumentCaptor.forClass(UUID.class);
        verify(writer).createSubmitted(id.capture(), any(), any(), any(), any());
        assertThat(count("applications")).isZero();
        assertThat(snapshots.read(id.getValue())).startsWith("%PDF-".getBytes());
    }
}
