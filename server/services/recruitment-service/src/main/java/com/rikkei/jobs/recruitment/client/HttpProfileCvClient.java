package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.rikkei.jobs.recruitment.dto.integration.CvPdf;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class HttpProfileCvClient implements ProfileCvClient {
    private final RestClient client;
    private final String path;
    @Autowired
    public HttpProfileCvClient(RestClient.Builder builder,
        @Value("${app.recruitment.profile-base-url}") String baseUrl,
        @Value("${app.recruitment.cv-pdf-path}") String path) {
        this(RecruitmentHttpSupport.client(builder, baseUrl), path);
    }
    public HttpProfileCvClient(RestClient client, String path) { this.client = client; this.path = path; }
    @Override public CvPdf exportOwnedPdf(UUID cvId, String candidateBearerToken) {
        try {
            var response = client.get().uri(path, cvId).header("Authorization", candidateBearerToken)
                .accept(MediaType.APPLICATION_PDF).retrieve().toEntity(byte[].class);
            var type = response.getHeaders().getContentType();
            byte[] bytes = response.getBody();
            if (type == null || !MediaType.APPLICATION_PDF.isCompatibleWith(type) || bytes == null || bytes.length < 5 ||
                bytes.length > 20 * 1024 * 1024 || !Arrays.equals(Arrays.copyOf(bytes, 5), "%PDF-".getBytes(StandardCharsets.US_ASCII))) {
                throw new RecruitmentDependencyException(502, "INVALID_CV_PDF", "Dịch vụ CV không trả về PDF hợp lệ.");
            }
            return new CvPdf(bytes);
        } catch (RestClientException ex) { throw RecruitmentHttpSupport.failure(ex, true); }
    }
}
