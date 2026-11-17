package com.rikkei.jobs.profile.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.rikkei.jobs.profile.config.SyncProperties;
import com.rikkei.jobs.profile.entity.MappingSource;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Gọi mock LMS, Khảo thí, lms-pm (docs/mock-contract.md): gắn X-Service-Key, timeout {@code app.sync.timeout},
 * thử lại {@code app.sync.retries} lần khi lỗi mạng hoặc 5xx. Lỗi 4xx (sai khoá, không có) không thử lại.
 */
@Slf4j
@Component
public class ExternalSourceClient {
    private static final String SERVICE_KEY_HEADER = "X-Service-Key";

    private final RestClient restClient;
    private final SyncProperties properties;

    public ExternalSourceClient(RestClient.Builder builder, SyncProperties properties) {
        this.properties = properties;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(properties.getTimeout()).build());
        factory.setReadTimeout(properties.getTimeout());
        this.restClient = builder.clone()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(SERVICE_KEY_HEADER, properties.getServiceKey())
                .requestFactory(factory)
                .build();
    }

    public List<JsonNode> lmsResults(String lmsUserId) {
        return fetch(MappingSource.LMS, "/lms/students/{id}/results", lmsUserId);
    }

    public List<JsonNode> examResults(String lmsUserId) {
        return fetch(MappingSource.EXAM, "/exam/students/{id}/results", lmsUserId);
    }

    public List<JsonNode> internships(String lmsUserId) {
        return fetch(MappingSource.PM, "/pm/students/{id}/internships", lmsUserId);
    }

    private List<JsonNode> fetch(MappingSource source, String path, String lmsUserId) {
        int attempts = properties.getRetries() + 1;
        for (int attempt = 1; ; attempt++) {
            try {
                JsonNode body = restClient.get().uri(path, lmsUserId).retrieve().body(JsonNode.class);
                if (body == null || !body.isArray()) {
                    throw new ExternalSourceException(source, label(source) + " trả dữ liệu không phải danh sách", null);
                }
                List<JsonNode> items = new ArrayList<>();
                body.forEach(items::add);
                return items;
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status < 500 || attempt >= attempts) {
                    throw new ExternalSourceException(source,
                            label(source) + " trả lỗi HTTP " + status + retryNote(attempt), e);
                }
                log.warn("{} {} trả {}, thử lại lần {}", source, path, status, attempt);
            } catch (RestClientException e) {
                if (attempt >= attempts) {
                    throw new ExternalSourceException(source,
                            "Không kết nối được " + label(source) + retryNote(attempt), e);
                }
                log.warn("{} {} lỗi kết nối ({}), thử lại lần {}", source, path, e.getMessage(), attempt);
            }
            sleepBeforeRetry(source);
        }
    }

    private void sleepBeforeRetry(MappingSource source) {
        try {
            Thread.sleep(properties.getRetryBackoff().toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalSourceException(source, "Đồng bộ bị huỷ khi đang chờ thử lại " + label(source), e);
        }
    }

    private static String retryNote(int attempts) {
        return attempts > 1 ? " (đã thử " + attempts + " lần)" : "";
    }

    public static String label(MappingSource source) {
        return switch (source) {
            case LMS -> "LMS";
            case EXAM -> "hệ thống Khảo thí";
            case PM -> "lms-pm";
        };
    }
}
