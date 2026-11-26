package com.rikkei.jobs.identity.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.identity.dto.AdminUserDtos.AdminUserItem;
import com.rikkei.jobs.identity.dto.AdminUserDtos.AdminUserSummary;
import com.rikkei.jobs.identity.dto.AdminUserDtos.LockUserRequest;
import com.rikkei.jobs.identity.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Quản trị tài khoản, qua gateway: /api/v1/admin/users/**. Chỉ ADMIN. */
@Tag(name = "Quản trị - Tài khoản")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize(Roles.ADMIN)
public class AdminUserController {
    private final AdminUserService service;

    @Operation(summary = "Danh sách tài khoản", description = "Vai trò: ADMIN. q tìm theo email, họ tên, mã LMS; lọc role, status (ACTIVE | LOCKED). Mới tạo trước.")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: phân trang, role hoặc status sai")
    @GetMapping
    public PageResponse<AdminUserItem> list(@RequestParam(required = false) String q,
                                            @RequestParam(required = false) Role role,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "20") int size) {
        return service.list(q, role, status, page, size);
    }

    @Operation(summary = "Số tài khoản theo vai trò", description = "Vai trò: ADMIN. Đủ khoá của mọi vai trò; locked là số tài khoản đang bị khoá.")
    @GetMapping("/summary")
    public AdminUserSummary summary() {
        return service.summary();
    }

    @Operation(summary = "Khoá tài khoản", description = "Vai trò: ADMIN. Bắt buộc lý do. Không khoá được chính mình hoặc tài khoản ADMIN khác.")
    @ApiResponse(responseCode = "409", description = "CONFLICT: tự khoá mình hoặc khoá tài khoản quản trị")
    @PatchMapping("/{id}/lock")
    public AdminUserItem lock(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal,
                              @Valid @RequestBody LockUserRequest request) {
        return service.lock(principal.getUserId(), id, request.reason());
    }

    @Operation(summary = "Mở khoá tài khoản", description = "Vai trò: ADMIN. Gọi lại khi đã mở vẫn trả 200.")
    @PatchMapping("/{id}/unlock")
    public AdminUserItem unlock(@PathVariable UUID id) {
        return service.unlock(id);
    }
}
