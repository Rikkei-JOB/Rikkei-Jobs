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
public class HttpMentorIdentityClient implements MentorIdentityClient {
    private record User(UUID userId, String email, String role, String status) {}
    private final RestClient client;
    private final String key;

    @Autowired
    public HttpMentorIdentityClient(RestClient.Builder builder,
        @Value("${app.recruitment.identity-base-url}") String baseUrl,
        @Value("${app.recruitment.identity-service-key:}") String key) {
        this(RecruitmentHttpSupport.client(builder, baseUrl), key);
    }

    public HttpMentorIdentityClient(RestClient client, String key) {
        this.client = client; this.key = key;
    }

    @Override public String email(UUID mentorId) {
        if (key == null || key.isBlank()) throw unavailable();
        try {
            User[] users = client.post().uri("/internal/users/lookup")
                .header("X-Internal-Key", key).body(Map.of("userIds", List.of(mentorId)))
                .retrieve().body(User[].class);
            if (users == null || users.length != 1 || users[0] == null
                || !mentorId.equals(users[0].userId()) || !"MENTOR".equals(users[0].role())
                || !"ACTIVE".equals(users[0].status()) || users[0].email() == null
                || users[0].email().isBlank()) throw unavailable();
            return users[0].email().strip();
        } catch (RestClientException ex) { throw unavailable(); }
    }

    private RecruitmentDependencyException unavailable() {
        return new RecruitmentDependencyException(503, "IDENTITY_UNAVAILABLE", "Không xác thực được email mentor.");
    }
}
