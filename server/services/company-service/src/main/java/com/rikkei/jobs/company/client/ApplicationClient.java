package com.rikkei.jobs.company.client;

import java.util.UUID;

/** Kiểm tra ứng viên đã ứng tuyển vào tin tuyển dụng hay chưa, qua recruitment-service. */
public interface ApplicationClient {

    /** GET /internal/applications/exists?jobId&userId. */
    boolean hasApplied(UUID jobId, UUID userId, String authorization);
}
