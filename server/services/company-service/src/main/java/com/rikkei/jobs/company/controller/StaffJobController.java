package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.request.ReasonRequest;
import com.rikkei.jobs.company.dto.response.StaffJobDetail;
import com.rikkei.jobs.company.dto.response.StaffJobItem;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.service.StaffJobReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cán bộ - Duyệt JD")
@RestController
@RequestMapping("/staff/jobs")
@RequiredArgsConstructor
public class StaffJobController {

    private final StaffJobReviewService reviewService;

    @Operation(
            summary = "Danh sách JD (cán bộ)",
            description = "Vai trò: STAFF. status tuỳ chọn (bỏ trống = mọi trạng thái), companyId tuỳ chọn. "
                    + "PENDING: gửi sớm nhất trước; trạng thái khác hoặc bỏ trống: mới cập nhật trước. "
                    + "page từ 1, size tối đa 100.")
    @ApiResponse(responseCode = "200", description = "Trang JD; submittedAt là lần cập nhật cuối của JD")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: page/size, status hoặc companyId không hợp lệ")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @GetMapping
    @PreAuthorize(Roles.STAFF)
    public PageResponse<StaffJobItem> list(@RequestParam(required = false) JobStatus status,
                                           @RequestParam(required = false) UUID companyId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return reviewService.list(status, companyId, page, size);
    }

    @Operation(
            summary = "Chi tiết JD (cán bộ)",
            description = "Vai trò: STAFF. Mọi trạng thái, mọi công ty (kể cả công ty bị khoá). "
                    + "reviewedBy.fullName null khi không tra được identity-service.")
    @ApiResponse(responseCode = "200", description = "Nội dung JD kèm trạng thái, lý do trả lại và người duyệt")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD")
    @GetMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    public StaffJobDetail detail(@PathVariable UUID id) {
        return reviewService.detail(id);
    }

    @Operation(
            summary = "Duyệt JD",
            description = "Vai trò: STAFF. Chỉ JD PENDING; xoá lý do trả lại cũ, ghi lịch sử APPROVE_JD. "
                    + "JD của công ty bị khoá vẫn duyệt được nhưng chỉ hiện khi công ty được mở khoá.")
    @ApiResponse(responseCode = "200", description = "JD đã chuyển sang APPROVED")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD")
    @ApiResponse(responseCode = "409",
            description = "INVALID_JOB_STATUS: JD không ở PENDING; JOB_EXPIRED: hạn nộp đã qua; "
                    + "CONFLICT: JD vừa được cập nhật ở nơi khác")
    @PostMapping("/{id}/approve")
    @PreAuthorize(Roles.STAFF)
    public StaffJobDetail approve(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return reviewService.approve(principal.getUserId(), id);
    }

    @Operation(
            summary = "Trả lại JD",
            description = "Vai trò: STAFF. Chỉ JD PENDING; lý do bắt buộc (trim, 1–1000 ký tự), HR thấy lý do và "
                    + "gửi lại được. Ghi lịch sử REJECT_JD.")
    @ApiResponse(responseCode = "200", description = "JD đã chuyển sang REJECTED")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: thiếu lý do hoặc quá 1000 ký tự (errors.reason)")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD")
    @ApiResponse(responseCode = "409",
            description = "INVALID_JOB_STATUS: JD không ở PENDING; CONFLICT: JD vừa được cập nhật ở nơi khác")
    @PostMapping("/{id}/reject")
    @PreAuthorize(Roles.STAFF)
    public StaffJobDetail reject(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request,
                                 @AuthenticationPrincipal AuthPrincipal principal) {
        return reviewService.reject(principal.getUserId(), id, request.reason());
    }

    @Operation(
            summary = "Đóng JD",
            description = "Vai trò: STAFF. Chỉ JD APPROVED (đóng do vi phạm); lý do bắt buộc (trim, 1–1000 ký tự), "
                    + "chỉ ghi vào lịch sử CLOSE_JD.")
    @ApiResponse(responseCode = "200", description = "JD đã chuyển sang CLOSED")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: thiếu lý do hoặc quá 1000 ký tự (errors.reason)")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD")
    @ApiResponse(responseCode = "409",
            description = "INVALID_JOB_STATUS: JD không ở APPROVED; CONFLICT: JD vừa được cập nhật ở nơi khác")
    @PostMapping("/{id}/close")
    @PreAuthorize(Roles.STAFF)
    public StaffJobDetail close(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request,
                                @AuthenticationPrincipal AuthPrincipal principal) {
        return reviewService.close(principal.getUserId(), id, request.reason());
    }
}
