package com.rikkei.jobs.profile.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

/** Uses identity UUID -> LMS identifier; service keys never leave the backend. */
@Component
public class CandidateProfileSource {
    private final RestClient identity;
    private final RestClient lms;
    private final String identityKey;
    private final String lmsKey;

    public CandidateProfileSource(
        @Value("${app.profile.identity-base-url}") String identityUrl,
        @Value("${app.profile.identity-service-key}") String identityKey,
        @Value("${app.profile.lms-base-url}") String lmsUrl,
        @Value("${app.profile.lms-service-key}") String lmsKey) {
        this.identityKey = identityKey; this.lmsKey = lmsKey;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        identity = RestClient.builder().baseUrl(identityUrl).requestFactory(factory).build();
        lms = RestClient.builder().baseUrl(lmsUrl).requestFactory(factory).build();
    }

    public InitialProfile fetch(UUID userId) {
        if (identityKey.isBlank() || lmsKey.isBlank()) throw ProfileSourceException.unavailable();
        JsonNode link = read(identity.get().uri("/internal/users/{id}/lms-identity", userId)
            .header("X-Internal-Key", identityKey));
        if (!userId.toString().equals(text(link, "userId", 36))) throw ProfileSourceException.invalid();
        String lmsId = text(link, "lmsUserId", 100);
        JsonNode student = read(lms.get().uri("/lms/students/{id}", lmsId).header("X-Service-Key", lmsKey));
        if (!lmsId.equals(text(student, "lmsUserId", 100))) throw ProfileSourceException.invalid();
        String email = text(student, "email", 255);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw ProfileSourceException.invalid();
        return new InitialProfile(text(student, "fullName", 255), email,
            text(student, "program", 255), text(student, "classCode", 100), text(student, "courseName", 255));
    }

    private static JsonNode read(RestClient.RequestHeadersSpec<?> request) {
        try {
            JsonNode data = request.retrieve().body(JsonNode.class);
            if (data == null || !data.isObject()) throw ProfileSourceException.invalid();
            return data;
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) throw new ResourceNotFoundException("Không tìm thấy hồ sơ LMS của ứng viên.");
            throw ProfileSourceException.unavailable();
        } catch (ResourceAccessException ex) {
            throw ProfileSourceException.unavailable();
        } catch (RestClientException ex) {
            throw ProfileSourceException.invalid();
        }
    }
    private static String text(JsonNode data, String field, int max) {
        JsonNode value = data.path(field);
        if (!value.isTextual() || value.asText().isBlank() || value.asText().length() > max)
            throw ProfileSourceException.invalid();
        return value.asText().strip();
    }
    public record InitialProfile(String fullName, String email, String program, String classCode, String courseName) { }
}
