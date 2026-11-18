package com.rikkei.jobs.company.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Không thử lại; mọi lỗi (kể cả khoá cấu hình rỗng) đều thành EXTERNAL_UNAVAILABLE. */
@Slf4j
@Component
public class HttpProfileSkillClient implements ProfileSkillClient {

    private static final String UNAVAILABLE = "Không lấy được điểm kỹ năng của ứng viên, vui lòng thử lại.";

    private final RestClient restClient;
    private final String serviceKey;

    public HttpProfileSkillClient(AppProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getProfile().getBaseUrl())
                .requestFactory(factory)
                .build();
        this.serviceKey = properties.getInternal().getServiceKey();
    }

    @Override
    public Map<UUID, BigDecimal> verifiedScores(UUID userId, String authorization) {
        if (serviceKey == null || serviceKey.isBlank()) {
            log.error("Thiếu app.internal.service-key, không gọi được profile-service");
            throw unavailable();
        }
        try {
            JsonNode body = restClient.get()
                    .uri("/internal/profiles/{userId}/verified-skills", userId)
                    .header("X-Internal-Key", serviceKey)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || !body.isArray()) {
                log.error("profile-service trả điểm kỹ năng rỗng hoặc sai định dạng");
                throw unavailable();
            }
            Map<UUID, BigDecimal> scores = new HashMap<>();
            for (JsonNode item : body) {
                scores.put(UUID.fromString(item.path("skillId").asText()), item.path("verifiedScore").decimalValue());
            }
            return scores;
        } catch (CompanyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Không lấy được điểm kỹ năng từ profile-service: {}", e.getMessage());
            throw unavailable();
        }
    }

    private static CompanyException unavailable() {
        return new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE, UNAVAILABLE);
    }
}
