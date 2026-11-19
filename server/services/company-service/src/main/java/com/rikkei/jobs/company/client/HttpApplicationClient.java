package com.rikkei.jobs.company.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Không thử lại; mọi lỗi (kể cả khoá cấu hình rỗng) đều thành EXTERNAL_UNAVAILABLE. */
@Slf4j
@Component
public class HttpApplicationClient implements ApplicationClient {

    private static final String UNAVAILABLE = "Không kiểm tra được đơn ứng tuyển, vui lòng thử lại.";

    private final RestClient restClient;
    private final String serviceKey;

    public HttpApplicationClient(AppProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getRecruitment().getBaseUrl())
                .requestFactory(factory)
                .build();
        this.serviceKey = properties.getInternal().getServiceKey();
    }

    @Override
    public boolean hasApplied(UUID jobId, UUID userId, String authorization) {
        if (serviceKey == null || serviceKey.isBlank()) {
            log.error("Thiếu app.internal.service-key, không gọi được recruitment-service");
            throw unavailable();
        }
        try {
            JsonNode body = restClient.get()
                    .uri(uri -> uri.path("/internal/applications/exists")
                            .queryParam("jobId", jobId)
                            .queryParam("userId", userId)
                            .build())
                    .header("X-Internal-Key", serviceKey)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || !body.path("applied").isBoolean()) {
                log.error("recruitment-service trả kết quả kiểm tra đơn ứng tuyển sai định dạng");
                throw unavailable();
            }
            return body.get("applied").asBoolean();
        } catch (CompanyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Không kiểm tra được đơn ứng tuyển ở recruitment-service: {}", e.getMessage());
            throw unavailable();
        }
    }

    private static CompanyException unavailable() {
        return new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE, UNAVAILABLE);
    }
}
