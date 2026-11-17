package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
        requireKey();
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

    @Override
    public Map<UUID, CandidateIdentity> getBatch(List<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        requireKey();
        try {
            var response = client.post().uri("/internal/users/candidates/lookup")
                .header("X-Internal-Key", serviceKey).body(userIds).retrieve().body(InternalUser[].class);
            if (response == null || response.length != userIds.stream().distinct().count())
                throw invalidIdentity();
            var users = java.util.Arrays.stream(response).map(user -> {
                if (user == null || !"CANDIDATE".equals(user.role()) || user.userId() == null
                    || user.email() == null || user.email().isBlank() || user.fullName() == null || user.fullName().isBlank())
                    throw invalidIdentity();
                return new CandidateIdentity(user.userId(), user.fullName(), user.email());
            }).collect(Collectors.toMap(CandidateIdentity::userId, Function.identity(), (a, b) -> { throw invalidIdentity(); }));
            if (!users.keySet().containsAll(userIds)) throw invalidIdentity();
            return users;
        } catch (RestClientException ex) {
            throw new RecruitmentDependencyException(503, "IDENTITY_UNAVAILABLE", "Không lấy được thông tin ứng viên.");
        }
    }

    private void requireKey() {
        if (serviceKey == null || serviceKey.isBlank())
            throw new RecruitmentDependencyException(503, "DEPENDENCY_NOT_CONFIGURED", "Kết nối identity chưa được cấu hình.");
    }

    private RecruitmentDependencyException invalidIdentity() {
        return new RecruitmentDependencyException(502, "INVALID_CANDIDATE_IDENTITY", "Thông tin ứng viên không hợp lệ.");
    }
}
