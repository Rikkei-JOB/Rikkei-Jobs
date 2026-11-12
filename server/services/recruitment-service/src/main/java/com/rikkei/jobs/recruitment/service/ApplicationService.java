package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.recruitment.dto.request.*;
import com.rikkei.jobs.recruitment.dto.response.*;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import java.util.UUID;
public interface ApplicationService {
    ApplicationDetailResponse apply(AuthPrincipal actor, String bearerToken, UUID jobId, ApplyRequest request);
    PageResponse<ApplicationSummaryResponse> listOwn(UUID userId, ApplicationStatus status, int page, int size);
    ApplicationDetailResponse getOwn(UUID userId, UUID id);
    ApplicationDetailResponse withdraw(AuthPrincipal actor, UUID id, WithdrawRequest request);
    byte[] readSnapshot(AuthPrincipal actor, UUID id);
}
