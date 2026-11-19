package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.company.dto.response.MatchResponse;
import com.rikkei.jobs.company.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Qua gateway: {@code /api/v1/jobs/{id}/match/**} (DN-06). recruitment-service (UT-02) gọi endpoint thứ hai. */
@Tag(name = "Điểm khớp JD")
@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobMatchController {

    private final MatchService matchService;

    @Operation(
            summary = "Điểm khớp của chính mình với JD",
            description = "Vai trò: CANDIDATE. Chỉ JD đã duyệt (APPROVED). Điểm tính từ điểm kỹ năng đã xác thực hiện tại.")
    @ApiResponse(responseCode = "200", description = "Điểm khớp, kỹ năng đạt và chưa đạt")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: id không phải UUID")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải ứng viên")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD hoặc JD chưa được duyệt")
    @ApiResponse(responseCode = "503", description = "EXTERNAL_UNAVAILABLE: không gọi được profile-service")
    @GetMapping("/{id}/match/me")
    @PreAuthorize(Roles.CANDIDATE)
    public MatchResponse me(
            @PathVariable UUID id,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return matchService.forCandidate(id, principal.getUserId(), authorization);
    }

    @Operation(
            summary = "Điểm khớp của một ứng viên với JD",
            description = "Vai trò: HR hoặc STAFF. HR chỉ xem JD của công ty mình và ứng viên đã ứng tuyển; "
                    + "STAFF xem mọi JD. Không thoả điều kiện thì trả 404 như JD không tồn tại.")
    @ApiResponse(responseCode = "200", description = "Điểm khớp, kỹ năng đạt và chưa đạt")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: id không phải UUID")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải HR hoặc STAFF")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD, JD của công ty khác hoặc ứng viên chưa ứng tuyển")
    @ApiResponse(responseCode = "503", description = "EXTERNAL_UNAVAILABLE: không gọi được profile-service hoặc recruitment-service")
    @GetMapping("/{id}/match/{userId}")
    @PreAuthorize("hasAnyRole('HR','STAFF')")
    public MatchResponse forUser(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return matchService.forViewer(id, userId, principal, authorization);
    }
}
