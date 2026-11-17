package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.company.dto.request.LockHrAccountRequest;
import com.rikkei.jobs.company.dto.response.StaffHrAccount;
import com.rikkei.jobs.company.service.HrAccountLockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Qua gateway: {@code /api/v1/staff/hr-accounts/**} (DN-05). */
@Tag(name = "Cán bộ - Tài khoản HR")
@RestController
@RequestMapping("/staff/hr-accounts")
@RequiredArgsConstructor
public class StaffHrAccountController {

    private final HrAccountLockService lockService;

    @Operation(
            summary = "Khoá tài khoản HR",
            description = "Vai trò: STAFF. Khoá ở identity-service rồi chép trạng thái khoá về company-service; khoá lại tài khoản đã khoá vẫn trả 200.")
    @ApiResponse(responseCode = "200", description = "Tài khoản HR sau khi khoá")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: lý do trống hoặc quá 500 ký tự")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: userId không phải tài khoản HR")
    @ApiResponse(responseCode = "503", description = "EXTERNAL_UNAVAILABLE: không kết nối được identity-service")
    @PostMapping("/{userId}/lock")
    @PreAuthorize(Roles.STAFF)
    public StaffHrAccount lock(@PathVariable UUID userId, @Valid @RequestBody LockHrAccountRequest request) {
        return lockService.lock(userId, request.reason());
    }

    @Operation(
            summary = "Mở khoá tài khoản HR",
            description = "Vai trò: STAFF. Mở khoá ở identity-service rồi xoá bản sao khoá ở company-service.")
    @ApiResponse(responseCode = "200", description = "Tài khoản HR sau khi mở khoá")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: userId không phải tài khoản HR")
    @ApiResponse(responseCode = "503", description = "EXTERNAL_UNAVAILABLE: không kết nối được identity-service")
    @PostMapping("/{userId}/unlock")
    @PreAuthorize(Roles.STAFF)
    public StaffHrAccount unlock(@PathVariable UUID userId) {
        return lockService.unlock(userId);
    }
}
