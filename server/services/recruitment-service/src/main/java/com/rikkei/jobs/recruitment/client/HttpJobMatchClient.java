package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.rikkei.jobs.recruitment.dto.response.MatchResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class HttpJobMatchClient implements JobMatchClient {
    private final RestClient client;

    @Autowired
    public HttpJobMatchClient(RestClient.Builder builder,
        @Value("${app.recruitment.match-base-url}") String baseUrl) {
        this(RecruitmentHttpSupport.client(builder, baseUrl));
    }

    public HttpJobMatchClient(RestClient client) { this.client = client; }

    @Override
    public Optional<MatchResponse> get(UUID jobId, UUID userId, String bearer) {
        try {
            var response = client.get().uri("/jobs/{id}/match/{userId}", jobId, userId)
                .header("Authorization", bearer).retrieve().body(MatchResponse.class);
            return Optional.ofNullable(response);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 404 || status >= 500) return Optional.empty();
            throw new RecruitmentDependencyException(502, "MATCH_CONTRACT_ERROR", "Không thể xác thực quyền đọc điểm khớp.");
        } catch (RestClientException ex) {
            return Optional.empty();
        }
    }
}
