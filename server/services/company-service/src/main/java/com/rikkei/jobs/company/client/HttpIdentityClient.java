package com.rikkei.jobs.company.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.config.AppProperties;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Không tự thử lại: tạo tài khoản không idempotent, nơi gọi xử lý qua nhánh EMAIL_EXISTS. */
@Slf4j
@Component
public class HttpIdentityClient implements IdentityClient {

    private static final String KEY_HEADER = "X-Internal-Key";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestClient restClient;

    public HttpIdentityClient(AppProperties properties) {
        // Dùng JDK HttpClient (như HttpLmsClient của identity) để đọc được phản hồi 401/4xx của POST.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getIdentity().getBaseUrl())
                .defaultHeader(KEY_HEADER, properties.getInternal().getServiceKey())
                .requestFactory(factory)
                .build();
    }

    @Override
    public CreatedUser createHrUser(String email, String fullName, UUID companyId) {
        try {
            CreatedUser created = restClient.post()
                    .uri("/internal/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "fullName", fullName, "role", "HR",
                            "companyId", companyId.toString()))
                    .retrieve()
                    .body(CreatedUser.class);
            if (created == null || created.userId() == null) {
                log.error("identity trả phản hồi tạo user rỗng");
                throw new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE);
            }
            return created;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            if (status == 409) {
                JsonNode body = parse(e.getResponseBodyAsString());
                if ("EMAIL_EXISTS".equals(text(body, "code"))) {
                    JsonNode errors = body.path("errors");
                    throw new EmailExistsException(
                            uuid(errors, "userId"), role(errors, "role"), uuid(errors, "companyId"));
                }
            }
            if (status == 400) {
                String message = text(parse(e.getResponseBodyAsString()), "message");
                throw new BadRequestException(message != null ? message : "Dữ liệu không hợp lệ");
            }
            log.error("identity tạo user lỗi với mã trạng thái {}", status);
            throw new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE);
        } catch (RestClientException e) {
            log.error("Không kết nối được identity-service: {}", e.getMessage());
            throw new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE);
        }
    }

    @Override
    public void deleteUser(UUID userId) {
        try {
            restClient.delete().uri("/internal/users/{id}", userId).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return;
            }
            log.error("identity xoá user lỗi với mã trạng thái {}", e.getStatusCode());
            throw new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE);
        } catch (RestClientException e) {
            log.error("Không kết nối được identity-service: {}", e.getMessage());
            throw new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE);
        }
    }

    @Override
    public IdentityUserStatus lockUser(UUID userId, String reason) {
        return callStatus(() -> restClient.patch()
                .uri("/internal/users/{id}/lock", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("reason", reason))
                .retrieve()
                .body(IdentityUserStatus.class));
    }

    @Override
    public IdentityUserStatus unlockUser(UUID userId) {
        return callStatus(() -> restClient.patch()
                .uri("/internal/users/{id}/unlock", userId)
                .retrieve()
                .body(IdentityUserStatus.class));
    }

    @Override
    public List<IdentityUserStatus> lookupUsers(List<UUID> userIds) {
        return callStatus(() -> restClient.post()
                .uri("/internal/users/lookup")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("userIds", userIds))
                .retrieve()
                .body(new ParameterizedTypeReference<List<IdentityUserStatus>>() { }));
    }

    /** Gọi identity và đổi lỗi HTTP/mạng sang ngoại lệ của service; phản hồi rỗng coi như identity lỗi. */
    private <T> T callStatus(Supplier<T> call) {
        try {
            T result = call.get();
            if (result == null) {
                log.error("identity trả phản hồi rỗng");
                throw new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE);
            }
            return result;
        } catch (RestClientResponseException e) {
            int status = e.getStatusCode().value();
            String message = text(parse(e.getResponseBodyAsString()), "message");
            if (status == 404) {
                throw new ResourceNotFoundException(message != null ? message : "Không tìm thấy tài khoản");
            }
            if (status == 409) {
                throw new ConflictException(message != null ? message : "Xung đột trạng thái tài khoản");
            }
            if (status == 400) {
                throw new BadRequestException(message != null ? message : "Dữ liệu không hợp lệ");
            }
            log.error("identity trả lỗi với mã trạng thái {}", status);
            throw new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE);
        } catch (RestClientException e) {
            log.error("Không kết nối được identity-service: {}", e.getMessage());
            throw new CompanyException(CompanyErrorCode.EXTERNAL_UNAVAILABLE);
        }
    }

    private static JsonNode parse(String body) {
        try {
            return JSON.readTree(body == null ? "" : body);
        } catch (Exception e) {
            return JSON.createObjectNode();
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private static UUID uuid(JsonNode node, String field) {
        String value = text(node, field);
        try {
            return value == null ? null : UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Role role(JsonNode node, String field) {
        String value = text(node, field);
        try {
            return value == null ? null : Role.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
