package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.common.config.exception.*;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.*;

final class RecruitmentHttpSupport {
    private RecruitmentHttpSupport() {}
    static RestClient client(RestClient.Builder builder, String baseUrl) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(30));
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
    static RuntimeException failure(RestClientException ex, boolean candidateAuth) {
        if (ex instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            if (status == 404) return new ResourceNotFoundException("Không tìm thấy JD hoặc CV.");
            if (candidateAuth && status == 403) return new ForbiddenException("Bạn không có quyền sử dụng CV này.");
            if (candidateAuth && status == 401) return new RecruitmentDependencyException(401, "UNAUTHORIZED", "Phiên đăng nhập không còn hợp lệ.");
            if (status < 500 && status != 401 && status != 403 && status != 429) {
                return new RecruitmentDependencyException(502, "INVALID_DEPENDENCY_RESPONSE", "Dữ liệu từ dịch vụ liên quan không hợp lệ.");
            }
        }
        return new RecruitmentDependencyException(503, "DEPENDENCY_UNAVAILABLE", "Dịch vụ JD hoặc CV chưa sẵn sàng. Vui lòng thử lại.");
    }
}
