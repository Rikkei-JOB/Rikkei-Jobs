package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.rikkei.jobs.recruitment.dto.response.MatchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
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
        @Value("${app.recruitment.match-base-url}") String baseUrl,
        @Value("${app.recruitment.match-connect-timeout-ms:2000}") int connectTimeout,
        @Value("${app.recruitment.match-read-timeout-ms:5000}") int readTimeout) {
        var factory=new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.client=builder.clone().baseUrl(baseUrl).requestFactory(factory).build();
    }

    public HttpJobMatchClient(RestClient client) { this.client = client; }

    @Override
    public Optional<MatchResponse> get(UUID jobId, UUID userId, String bearer) {
        try {
            var response = client.get().uri("/jobs/{id}/match/{userId}", jobId, userId)
                .header("Authorization", bearer).retrieve().body(JsonNode.class);
            if (response == null || !response.isObject() || !response.path("score").isNumber()
                || response.path("score").decimalValue().signum() < 0
                || response.path("score").decimalValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0
                || !response.path("belowMinimum").isBoolean()) {
                throw contractError();
            }
            return Optional.of(new MatchResponse(response.path("score").decimalValue(),
                response.path("belowMinimum").booleanValue(), skills(response.path("met")), skills(response.path("missing"))));
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

    private List<MatchResponse.SkillMatchResponse> skills(JsonNode values) {
        if (!values.isArray()) throw contractError();
        var result = new ArrayList<MatchResponse.SkillMatchResponse>();
        for (var skill : values) {
            if (!skill.isObject() || !skill.path("skill").isTextual() || skill.path("skill").textValue().isBlank()
                || !skill.path("required").isBoolean() || !skill.path("minScore").isNumber()
                || skill.path("minScore").decimalValue().signum() <= 0 || !skill.path("candidateScore").isNumber()
                || skill.path("candidateScore").decimalValue().signum() < 0) throw contractError();
            result.add(new MatchResponse.SkillMatchResponse(skill.path("skill").textValue(), skill.path("required").booleanValue(),
                skill.path("minScore").decimalValue(), skill.path("candidateScore").decimalValue()));
        }
        return List.copyOf(result);
    }

    private RecruitmentDependencyException contractError() {
        return new RecruitmentDependencyException(502, "MATCH_CONTRACT_ERROR", "Dữ liệu điểm khớp không hợp lệ.");
    }
}
