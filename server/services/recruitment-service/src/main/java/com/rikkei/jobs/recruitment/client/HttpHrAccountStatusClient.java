package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpHrAccountStatusClient implements HrAccountStatusClient {
    private record UserStatus(UUID userId, String role, UUID companyId, String status) {}
    private final RestClient client;
    private final String key;

    @Autowired
    public HttpHrAccountStatusClient(RestClient.Builder builder,
        @Value("${app.recruitment.identity-base-url}") String baseUrl,
        @Value("${app.recruitment.identity-service-key:}") String key) {
        this(RecruitmentHttpSupport.client(builder, baseUrl), key);
    }

    public HttpHrAccountStatusClient(RestClient client, String key) {
        this.client = client;
        this.key = key;
    }

    @Override
    public State state(UUID userId, UUID companyId) {
        if (key == null || key.isBlank()) throw unavailable();
        if (userId == null || companyId == null) return State.LOCKED;
        try {
            var response = client.post().uri("/internal/users/lookup")
                .header("X-Internal-Key", key).body(Map.of("userIds", List.of(userId)))
                .retrieve().body(UserStatus[].class);
            if (response == null || response.length != 1) return State.LOCKED;
            var user = response[0];
            if (user == null || !userId.equals(user.userId()) || !companyId.equals(user.companyId())
                || !"HR".equals(user.role())) return State.LOCKED;
            if ("LOCKED".equals(user.status())) return State.LOCKED;
            if ("ACTIVE".equals(user.status())) return State.ACTIVE;
            throw unavailable();
        } catch (RestClientException ex) {
            throw unavailable();
        }
    }

    private RecruitmentDependencyException unavailable() {
        return new RecruitmentDependencyException(503, "IDENTITY_UNAVAILABLE", "Không xác thực được trạng thái tài khoản HR.");
    }
}
