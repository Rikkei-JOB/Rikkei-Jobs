package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.recruitment.dto.response.PingResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint kiểm tra nhanh: xác nhận JWT từ identity-service được chấp nhận và cho biết người gọi là ai.
 * Qua gateway: GET /api/v1/recruitment/ping. Dùng làm mẫu cho controller mới.
 */
@RestController
@RequestMapping("/recruitment")
public class RecruitmentPingController {

    @GetMapping("/ping")
    @PreAuthorize("isAuthenticated()")
    public PingResponse ping(@AuthenticationPrincipal AuthPrincipal principal) {
        PingResponse response = PingResponse.builder()
                .service("recruitment-service")
                .userId(principal.getUserId())
                .role(principal.getRole())
                .companyId(principal.getCompanyId())
                .build();
        return response;
    }
}
