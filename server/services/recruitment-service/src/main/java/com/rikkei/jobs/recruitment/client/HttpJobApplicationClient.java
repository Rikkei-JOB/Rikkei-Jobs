package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.rikkei.jobs.recruitment.dto.integration.JobApplicationContext;
import java.util.UUID;
import org.springframework.beans.factory.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;

@Component
public class HttpJobApplicationClient implements JobApplicationClient {
    private final RestClient client;
    private final String path;
    private final String serviceKey;

    @Autowired
    public HttpJobApplicationClient(RestClient.Builder builder,
        @Value("${app.recruitment.company-base-url}") String baseUrl,
        @Value("${app.recruitment.job-context-path}") String path,
        @Value("${app.recruitment.company-service-key:}") String serviceKey) {
        this(RecruitmentHttpSupport.client(builder, baseUrl), path, serviceKey);
    }
    public HttpJobApplicationClient(RestClient client, String path, String serviceKey) {
        this.client = client; this.path = path; this.serviceKey = serviceKey;
    }
    @Override public JobApplicationContext getContext(UUID jobId) {
        if (serviceKey == null || serviceKey.isBlank()) throw new RecruitmentDependencyException(503, "DEPENDENCY_NOT_CONFIGURED", "Kết nối dịch vụ JD chưa được cấu hình.");
        try { return client.get().uri(path, jobId).header("X-Service-Key", serviceKey).retrieve().body(JobApplicationContext.class); }
        catch (RestClientException ex) { throw RecruitmentHttpSupport.failure(ex, false); }
    }
}
