package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class HttpCandidateContactClient implements CandidateContactClient {
    private record Contact(UUID userId, String phone) {}
    private final RestClient client;
    private final String key;

    @Autowired
    public HttpCandidateContactClient(RestClient.Builder builder,
        @Value("${app.recruitment.profile-base-url}") String baseUrl,
        @Value("${app.recruitment.profile-service-key:}") String key) {
        this(RecruitmentHttpSupport.client(builder, baseUrl), key);
    }

    public HttpCandidateContactClient(RestClient client, String key) {
        this.client = client; this.key = key;
    }

    @Override public String phone(UUID userId, String bearer) {
        if (key == null || key.isBlank()) throw unavailable();
        try {
            Contact response = client.get().uri("/internal/profiles/{id}/phone", userId)
                .header("X-Internal-Key", key).header("Authorization", bearer)
                .retrieve().body(Contact.class);
            if (response == null || !userId.equals(response.userId()))
                throw new RecruitmentDependencyException(502, "INVALID_PROFILE_CONTACT", "Thông tin liên hệ không hợp lệ.");
            return response.phone();
        } catch (RestClientException ex) { throw unavailable(); }
    }

    private RecruitmentDependencyException unavailable() {
        return new RecruitmentDependencyException(503, "PROFILE_UNAVAILABLE", "Không lấy được số điện thoại ứng viên.");
    }
}
