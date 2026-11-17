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
import org.springframework.web.client.ResourceAccessException;

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
            if (response == null || response.score() == null || response.score().signum() < 0
                || response.score().compareTo(java.math.BigDecimal.valueOf(100)) > 0
                || response.met() == null || response.missing() == null
                || java.util.stream.Stream.concat(response.met().stream(), response.missing().stream())
                    .anyMatch(skill -> skill == null || skill.skill() == null || skill.skill().isBlank()
                        || skill.minScore() == null || skill.minScore().signum() <= 0
                        || skill.candidateScore() == null || skill.candidateScore().signum() < 0)) {
                throw contractError();
            }
            return Optional.of(response);
        } catch (RestClientResponseException ex) {
            int status = ex.getStatusCode().value();
            if (status == 404 || status >= 500) return Optional.empty();
            throw contractError();
        } catch (ResourceAccessException ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw contractError();
        }
    }

    private RecruitmentDependencyException contractError() {
        return new RecruitmentDependencyException(502, "MATCH_CONTRACT_ERROR", "Dữ liệu điểm khớp không hợp lệ.");
    }
}
