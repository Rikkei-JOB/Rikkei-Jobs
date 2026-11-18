package com.rikkei.jobs.company.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Không thử lại; mọi lỗi mạng, timeout, status không 2xx hoặc body rỗng đều thành EXTERNAL_UNAVAILABLE. */
@Slf4j
@Component
public class HttpSkillCatalogClient implements SkillCatalogClient {

    private static final String UNAVAILABLE = "Không kiểm tra được danh mục kỹ năng, vui lòng thử lại.";

    private final RestClient restClient;

    public HttpSkillCatalogClient(AppProperties properties) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getProfile().getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    @Override
    public Set<UUID> skillIds(String authorization) {
        try {
            JsonNode body = restClient.get()
                    .uri("/skills")
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve()
                    .body(JsonNode.class);
            if (body == null || !body.isArray()) {
                log.error("profile-service trả danh mục kỹ năng rỗng hoặc sai định dạng");
                throw unavailable();
            }
            Set<UUID> ids = new HashSet<>();
            for (JsonNode item : body) {
                ids.add(UUID.fromString(item.path("id").asText()));
            }
            return ids;
        } catch (CompanyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Không lấy được danh mục kỹ năng từ profile-service: {}", e.getMessage());
            throw unavailable();
        }
    }

    private static CompanyException unavailable() {
        return new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE, UNAVAILABLE);
    }
}
