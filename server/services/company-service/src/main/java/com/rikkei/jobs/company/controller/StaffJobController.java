package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.request.ReasonRequest;
import com.rikkei.jobs.company.dto.request.RejectJobRequest;
import com.rikkei.jobs.company.dto.response.StaffJobDtos.StaffJobDetail;
import com.rikkei.jobs.company.dto.response.StaffJobDtos.StaffJobItem;
import com.rikkei.jobs.company.dto.response.StaffJobDtos.StaffJobSummary;
import com.rikkei.jobs.company.entity.JobStatus;
import com.rikkei.jobs.company.service.StaffJobService;
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

/**
 * Qua gateway: /api/v1/staff/jobs/**. Cán bộ (và quản trị, theo ma trận quyền NT-01) duyệt, trả lại hoặc đóng JD
 * HR đã gửi. Hàng chờ và duyệt / trả lại đến từ UI-01; DN-07 thêm "đóng JD" kèm lý do, mã JOB_EXPIRED và việc ghi
 * lịch sử company_actions cho cả ba thao tác. Khoá / mở khoá công ty và trang lịch sử nằm ở StaffCompanyController
 * (chỉ STAFF).
 */
@Tag(name = "Cán bộ - Duyệt JD")
@RestController
@RequestMapping("/staff/jobs")
@RequiredArgsConstructor
@PreAuthorize(Roles.STAFF_OR_ADMIN)
public class StaffJobController {

    private final StaffJobService service;

    @Operation(summary = "Danh sách JD (cán bộ)", description = "Vai trò: STAFF hoặc ADMIN. Không có JD nháp. status lọc trạng thái; "
            + "q tìm theo tiêu đề, vị trí, tên công ty; companyId lọc theo công ty (tab \"JD của công ty\" của DN-07). PENDING xếp gửi "
            + "sớm trước (hàng chờ), trạng thái khác mới cập nhật trước. page từ 1, size tối đa 100.")
    @ApiResponse(responseCode = "200", description = "Trang JD {items, total, page, size}")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: phân trang sai, status sai hoặc DRAFT")
    @GetMapping
    public PageResponse<StaffJobItem> list(@RequestParam(required = false) JobStatus status,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(required = false) UUID companyId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        return service.list(status, q, companyId, page, size);
    }

    @Operation(summary = "Số JD theo trạng thái", description = "Vai trò: STAFF hoặc ADMIN. Đủ khoá PENDING, APPROVED, REJECTED, CLOSED "
            + "(không tính JD nháp).")
    @GetMapping("/summary")
    public StaffJobSummary summary() {
        return service.summary();
    }

    @Operation(summary = "Chi tiết JD (cán bộ)", description = "Vai trò: STAFF hoặc ADMIN. Mọi công ty, kể cả công ty đang bị khoá "
            + "(company.status = LOCKED). JD nháp trả 404. reviewedBy là userId cán bộ xử lý gần nhất, null khi chưa ai xử lý.")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD hoặc JD đang là nháp")
    @GetMapping("/{id}")
    public StaffJobDetail get(@PathVariable UUID id) {
        return service.get(id);
    }

    @Operation(summary = "Duyệt JD", description = "Vai trò: STAFF hoặc ADMIN. PENDING -> APPROVED, xoá lý do trả lại cũ, "
            + "ghi lịch sử công ty APPROVE_JD (DN-07). JD quá hạn nộp không duyệt được. JD của công ty bị khoá vẫn duyệt được nhưng "
            + "chỉ hiện ở trang việc làm khi công ty được mở khoá.")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF/ADMIN")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD hoặc JD đang là nháp")
    @ApiResponse(responseCode = "409", description = "INVALID_JOB_STATUS: JD không chờ duyệt hoặc cán bộ khác vừa xử lý; "
            + "JOB_EXPIRED: hạn nộp đã qua; CONFLICT: JD vừa được cập nhật ở nơi khác")
    @PostMapping("/{id}/approve")
    public StaffJobDetail approve(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return service.approve(principal.getUserId(), id);
    }

    @Operation(summary = "Trả lại JD", description = "Vai trò: STAFF hoặc ADMIN. PENDING -> REJECTED kèm lý do (bắt buộc, trim, "
            + "1-1000 ký tự), ghi lịch sử công ty REJECT_JD (DN-07). HR thấy lý do trên form JD và gửi duyệt lại được.")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: thiếu hoặc quá dài lý do (errors.reason)")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF/ADMIN")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD hoặc JD đang là nháp")
    @ApiResponse(responseCode = "409", description = "INVALID_JOB_STATUS: JD không chờ duyệt; CONFLICT: JD vừa được cập nhật ở nơi khác")
    @PostMapping("/{id}/reject")
    public StaffJobDetail reject(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal,
                                 @Valid @RequestBody RejectJobRequest request) {
        return service.reject(principal.getUserId(), id, request.reason());
    }

    @Operation(summary = "Đóng JD", description = "Vai trò: STAFF hoặc ADMIN (DN-07). APPROVED -> CLOSED (đóng JD vi phạm) kèm lý do "
            + "(bắt buộc, trim, 1-1000 ký tự). Lý do chỉ ghi vào lịch sử công ty (CLOSE_JD), rejectReason không đổi. Thao tác này ghi đè "
            + "reviewedBy/reviewedAt bằng người đóng; người duyệt trước đó chỉ còn trong lịch sử công ty.")
    @ApiResponse(responseCode = "200", description = "JD đã chuyển sang CLOSED")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: thiếu hoặc quá dài lý do (errors.reason)")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF/ADMIN")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD hoặc JD đang là nháp")
    @ApiResponse(responseCode = "409", description = "INVALID_JOB_STATUS: JD không ở APPROVED; CONFLICT: JD vừa được cập nhật ở nơi khác")
    @PostMapping("/{id}/close")
    public StaffJobDetail close(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal,
                                @Valid @RequestBody ReasonRequest request) {
        return service.close(principal.getUserId(), id, request.reason());
    }
}
