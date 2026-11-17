package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.exception.ForbiddenException;
import com.rikkei.jobs.recruitment.client.*;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ApplicationDependencyClientsTest {
    @Test void profileForwardsCandidateTokenAndOnlyAcceptsPdf() {
        var builder = RestClient.builder().baseUrl("http://profile");
        var mock = MockRestServiceServer.bindTo(builder).build();
        var client = new HttpProfileCvClient(builder.build(), "/profile/cvs/{cvId}/pdf");
        var id = UUID.randomUUID();
        mock.expect(requestTo("http://profile/profile/cvs/" + id + "/pdf")).andExpect(header("Authorization", "Bearer candidate"))
            .andRespond(withSuccess("%PDF-1.7\noriginal", MediaType.APPLICATION_PDF));
        assertThat(client.exportOwnedPdf(id, "Bearer candidate").bytes()).startsWith("%PDF-".getBytes());
        mock.verify();
        mock.reset();
        mock.expect(anything()).andRespond(withSuccess("html", MediaType.TEXT_HTML));
        assertThatThrownBy(() -> client.exportOwnedPdf(id, "Bearer candidate")).isInstanceOf(RecruitmentDependencyException.class)
            .extracting("status").isEqualTo(502);
    }
    @Test void wrongCvOwnerAndUnavailableDependencyAreNotValidationErrors() {
        var builder = RestClient.builder().baseUrl("http://profile");
        var mock = MockRestServiceServer.bindTo(builder).build();
        var client = new HttpProfileCvClient(builder.build(), "/profile/cvs/{cvId}/pdf");
        mock.expect(anything()).andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> client.exportOwnedPdf(UUID.randomUUID(), "Bearer candidate")).isInstanceOf(ForbiddenException.class);
        mock.reset();
        mock.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> client.exportOwnedPdf(UUID.randomUUID(), "Bearer candidate")).isInstanceOf(RecruitmentDependencyException.class)
            .extracting("status").isEqualTo(503);
    }
    @Test void jobContextUsesInternalEndpointAndCredential() {
        var builder = RestClient.builder().baseUrl("http://company");
        var mock = MockRestServiceServer.bindTo(builder).build();
        var client = new HttpJobApplicationClient(builder.build(), "/internal/jobs/{jobId}/application-context", "test-key");
        var id = UUID.randomUUID();
        mock.expect(requestTo("http://company/internal/jobs/" + id + "/application-context"))
            .andExpect(header("X-Internal-Key", "test-key"))
            .andRespond(withSuccess("{\"jobId\":\"" + id + "\",\"companyId\":\"" + UUID.randomUUID()
                + "\",\"jobTitle\":\"Java\",\"companyName\":\"Company\",\"status\":\"APPROVED\",\"deadline\":\"2030-01-01T00:00:00Z\",\"companyLocked\":false,\"hrEmails\":[]}", MediaType.APPLICATION_JSON));
        assertThat(client.getContext(id).jobId()).isEqualTo(id);
        mock.verify();
    }
    @Test void missingServiceKeyFailsClosedWithoutCallingCompany() {
        var client = new HttpJobApplicationClient(RestClient.create("http://company"), "/unused/{jobId}", "");
        assertThatThrownBy(() -> client.getContext(UUID.randomUUID())).isInstanceOf(RecruitmentDependencyException.class)
            .extracting("status").isEqualTo(503);
    }

    @Test void identityBatchUsesServiceKeyAndRejectsNonCandidate() {
        var builder = RestClient.builder().baseUrl("http://identity");
        var mock = MockRestServiceServer.bindTo(builder).build();
        var client = new HttpIdentityCandidateClient(builder.build(), "test-key");
        var id = UUID.randomUUID();
        mock.expect(requestTo("http://identity/internal/users/candidates/lookup"))
            .andExpect(header("X-Internal-Key", "test-key"))
            .andRespond(withSuccess("[{\"userId\":\"" + id + "\",\"email\":\"a@example.test\",\"fullName\":\"Ứng viên A\",\"role\":\"CANDIDATE\"}]", MediaType.APPLICATION_JSON));
        assertThat(client.getBatch(java.util.List.of(id)).get(id).fullName()).isEqualTo("Ứng viên A");
        mock.verify(); mock.reset();
        mock.expect(anything()).andRespond(withSuccess("[{\"userId\":\"" + id + "\",\"email\":\"h@example.test\",\"fullName\":\"HR\",\"role\":\"HR\"}]", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.getBatch(java.util.List.of(id)))
            .isInstanceOf(RecruitmentDependencyException.class).extracting("status").isEqualTo(502);
    }

    @Test void matchClientKeepsAuthFailuresVisibleAndAllowsUnavailableScore() {
        var builder = RestClient.builder().baseUrl("http://match");
        var mock = MockRestServiceServer.bindTo(builder).build();
        var client = new HttpJobMatchClient(builder.build());
        var jobId = UUID.randomUUID(); var userId = UUID.randomUUID();
        mock.expect(requestTo("http://match/jobs/" + jobId + "/match/" + userId))
            .andExpect(header("Authorization", "Bearer hr"))
            .andRespond(withSuccess("{\"score\":80,\"belowMinimum\":false,\"met\":[],\"missing\":[]}", MediaType.APPLICATION_JSON));
        assertThat(client.get(jobId, userId, "Bearer hr")).get().extracting("score").isEqualTo(new java.math.BigDecimal("80"));
        mock.verify(); mock.reset();
        mock.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThat(client.get(jobId, userId, "Bearer hr")).isEmpty();
        mock.reset();
        mock.expect(anything()).andRespond(withStatus(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> client.get(jobId, userId, "Bearer hr"))
            .isInstanceOf(RecruitmentDependencyException.class).extracting("status").isEqualTo(502);
        mock.reset();
        mock.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.get(jobId, userId, "Bearer hr"))
            .isInstanceOf(RecruitmentDependencyException.class).extracting("status").isEqualTo(502);
    }

    @Test void hrAccountStatusUsesDn05ContractAndFailsClosed() {
        var builder = RestClient.builder().baseUrl("http://identity");
        var mock = MockRestServiceServer.bindTo(builder).build();
        var client = new HttpHrAccountStatusClient(builder.build(), "test-key");
        var id = UUID.randomUUID(); var company = UUID.randomUUID();
        mock.expect(requestTo("http://identity/internal/users/lookup"))
            .andExpect(header("X-Internal-Key", "test-key"))
            .andRespond(withSuccess("[{\"userId\":\"" + id + "\",\"role\":\"HR\",\"companyId\":\"" + company + "\",\"status\":\"ACTIVE\"}]", MediaType.APPLICATION_JSON));
        assertThat(client.state(id, company)).isEqualTo(HrAccountStatusClient.State.ACTIVE);
        mock.verify(); mock.reset();
        mock.expect(anything()).andRespond(withSuccess("[{\"userId\":\"" + id + "\",\"role\":\"HR\",\"companyId\":\"" + company + "\",\"status\":\"LOCKED\"}]", MediaType.APPLICATION_JSON));
        assertThat(client.state(id, company)).isEqualTo(HrAccountStatusClient.State.LOCKED);
        mock.reset();
        mock.expect(anything()).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
        assertThat(client.state(id, company)).isEqualTo(HrAccountStatusClient.State.LOCKED);
        mock.reset();
        mock.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        assertThatThrownBy(() -> client.state(id, company)).isInstanceOf(RecruitmentDependencyException.class)
            .extracting("status").isEqualTo(503);
    }
}
