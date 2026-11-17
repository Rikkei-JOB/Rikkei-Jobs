package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpIdentityCandidateClient implements IdentityCandidateClient {
    private final RestClient client;
    private final String serviceKey;
    private record InternalUser(UUID userId, String email, String fullName, String role, UUID companyId) {}

    @Autowired
    public HttpIdentityCandidateClient(RestClient.Builder builder,
        @Value("${app.recruitment.identity-base-url}") String baseUrl,
        @Value("${app.recruitment.identity-service-key:}") String serviceKey) {
        this(RecruitmentHttpSupport.client(builder, baseUrl), serviceKey);
    }

    public HttpIdentityCandidateClient(RestClient client, String serviceKey) {
        this.client = client;
        this.serviceKey = serviceKey;
    }

    @Override
    public CandidateIdentity get(UUID userId) {
        if (serviceKey == null || serviceKey.isBlank())
            throw new RecruitmentDependencyException(503, "DEPENDENCY_NOT_CONFIGURED", "Kết nối identity chưa được cấu hình.");
        try {
            var response = client.get().uri("/internal/users/{id}", userId)
                .header("X-Internal-Key", serviceKey).retrieve().body(InternalUser.class);
            if (response == null || !userId.equals(response.userId()) || !"CANDIDATE".equals(response.role())
                || response.email() == null || response.email().isBlank()
                || response.fullName() == null || response.fullName().isBlank()) {
                throw new RecruitmentDependencyException(502, "INVALID_CANDIDATE_IDENTITY", "Thông tin ứng viên không hợp lệ.");
            }
            return new CandidateIdentity(response.userId(), response.fullName(), response.email());
        } catch (RestClientException ex) {
            throw new RecruitmentDependencyException(503, "IDENTITY_UNAVAILABLE", "Không lấy được thông tin ứng viên.");
        }
    }
}
