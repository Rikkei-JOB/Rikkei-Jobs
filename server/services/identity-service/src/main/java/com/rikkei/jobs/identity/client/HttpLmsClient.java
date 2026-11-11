package com.rikkei.jobs.identity.client;

import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.config.AppProperties;
import com.rikkei.jobs.identity.config.exception.ApiException;
import com.rikkei.jobs.identity.config.exception.ErrorCode;
import com.rikkei.jobs.identity.dto.request.LmsVerifyRequest;
import com.rikkei.jobs.identity.dto.response.DevSsoTicketResponse;
import com.rikkei.jobs.identity.dto.response.LmsTicketInfo;
import com.rikkei.jobs.identity.dto.response.LmsVerifyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class HttpLmsClient implements LmsClient {
    private static final String SERVICE_KEY_HEADER = "X-Service-Key";

    private final RestClient restClient;

    public HttpLmsClient(AppProperties properties) {
        // Dùng JDK HttpClient thay cho HttpURLConnection vì loại này không đọc được phản hồi 401 của POST.
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getLms().getBaseUrl())
                .defaultHeader(SERVICE_KEY_HEADER, properties.getLms().getServiceKey())
                .requestFactory(factory)
                .build();
    }

    @Override
    public Optional<LmsTicketInfo> verify(String ticket) {
        try {
            LmsVerifyResponse response = restClient.post()
                    .uri("/lms/sso/verify")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new LmsVerifyRequest(ticket))
                    .retrieve()
                    .body(LmsVerifyResponse.class);
            if (response == null || response.getLmsUserId() == null || response.getEmail() == null
                    || response.getRole() == null || response.getIssuedAt() == null) {
                return Optional.empty();
            }
            return Optional.of(new LmsTicketInfo(
                    response.getLmsUserId(), response.getEmail(), response.getFullName(),
                    parseRole(response.getRole()), response.getIssuedAt()));
        } catch (RestClientResponseException e) {
            // Mock trả 404 NOT_FOUND khi vé không tồn tại; lỗi khác (401 sai khoá, 5xx) là lỗi cấu hình/hạ tầng.
            if (e.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            log.error("LMS verify lỗi với mã trạng thái {}", e.getStatusCode());
            throw new ApiException(ErrorCode.LMS_UNAVAILABLE);
        } catch (RestClientException e) {
            log.error("Không kết nối được LMS: {}", e.getMessage());
            throw new ApiException(ErrorCode.LMS_UNAVAILABLE);
        }
    }

    @Override
    public DevSsoTicketResponse issueTicket(String lmsUserId) {
        try {
            DevSsoTicketResponse response = restClient.post()
                    .uri("/lms/sso/tickets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("lmsUserId", lmsUserId))
                    .retrieve()
                    .body(DevSsoTicketResponse.class);
            if (response == null || response.ticket() == null) {
                throw new ApiException(ErrorCode.LMS_UNAVAILABLE);
            }
            return response;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new ApiException(ErrorCode.INVALID_TICKET, "Không có người dùng LMS: " + lmsUserId);
            }
            log.error("LMS cấp vé lỗi với mã trạng thái {}", e.getStatusCode());
            throw new ApiException(ErrorCode.LMS_UNAVAILABLE);
        } catch (RestClientException e) {
            log.error("Không kết nối được LMS: {}", e.getMessage());
            throw new ApiException(ErrorCode.LMS_UNAVAILABLE);
        }
    }

    private Role parseRole(String raw) {
        try {
            return Role.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
