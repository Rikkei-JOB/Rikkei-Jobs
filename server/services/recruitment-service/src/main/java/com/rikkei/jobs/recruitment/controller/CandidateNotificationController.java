package com.rikkei.jobs.recruitment.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.recruitment.service.CandidateNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/recruitment/me/notifications")
@RequiredArgsConstructor @PreAuthorize(Roles.CANDIDATE)
public class CandidateNotificationController {
    private final CandidateNotificationService notifications;

    @GetMapping
    @Operation(summary = "Ứng viên xem thông báo của mình", description = "CANDIDATE. page từ 1; size 1–100. unreadCount là tổng chưa đọc. 400 query sai, 401 chưa đăng nhập, 403 sai vai trò.")
    public CandidateNotificationService.Feed list(@AuthenticationPrincipal AuthPrincipal actor,
        @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notifications.list(actor.getUserId(), page, size, unreadOnly);
    }

    @PostMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Đánh dấu thông báo đã đọc", description = "CANDIDATE, idempotent; 404 nếu không có hoặc thuộc người khác; 401/403 khi không có quyền.")
    public void read(@AuthenticationPrincipal AuthPrincipal actor, @PathVariable UUID id) { notifications.read(actor.getUserId(), id); }

    @PostMapping("/read-all") @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Đánh dấu tất cả thông báo của mình đã đọc", description = "CANDIDATE, idempotent; 401/403 khi không có quyền.")
    public void readAll(@AuthenticationPrincipal AuthPrincipal actor) { notifications.readAll(actor.getUserId()); }
}
