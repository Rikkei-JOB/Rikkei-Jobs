package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.company.dto.request.CreateCompanyRequest;
import com.rikkei.jobs.company.dto.request.CreateHrAccountRequest;
import com.rikkei.jobs.company.dto.request.ReasonRequest;
import com.rikkei.jobs.company.dto.response.CompanyResponse;
import com.rikkei.jobs.company.dto.response.HrAccountResponse;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.response.StaffCompanyDetail;
import com.rikkei.jobs.company.dto.response.StaffCompanyItem;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.dto.response.CompanyActionItem;
import com.rikkei.jobs.company.service.CompanyHistoryService;
import com.rikkei.jobs.company.service.CompanyLockService;
import com.rikkei.jobs.company.service.CompanyService;
import com.rikkei.jobs.company.service.HrAccountService;
import com.rikkei.jobs.company.service.StaffCompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Cán bộ - Doanh nghiệp")
@RestController
@RequestMapping("/staff/companies")
@RequiredArgsConstructor
public class StaffCompanyController {

    private final CompanyService companyService;
    private final HrAccountService hrAccountService;
    private final StaffCompanyService staffCompanyService;
    private final CompanyLockService companyLockService;
    private final CompanyHistoryService companyHistoryService;

    @Operation(
            summary = "Danh sách công ty (cán bộ)",
            description = "Vai trò: STAFF. Tìm theo tên hoặc mã số thuế (q), lọc theo trạng thái; page từ 1, size tối đa 100; mới nhất trước.")
    @ApiResponse(responseCode = "200", description = "Trang công ty kèm số HR (jdCount là số JD không phải bản nháp)")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: page/size hoặc status không hợp lệ")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @GetMapping
    @PreAuthorize(Roles.STAFF)
    public PageResponse<StaffCompanyItem> list(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) CompanyStatus status,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return staffCompanyService.search(q, status, page, size);
    }

    @Operation(
            summary = "Chi tiết công ty (cán bộ)",
            description = "Vai trò: STAFF. Kèm danh sách tài khoản HR lấy từ identity-service (status = khoá riêng, "
                    + "companyLocked = khoá theo công ty) và lock (null khi ACTIVE; lockedBy.fullName null khi không tra "
                    + "được identity-service).")
    @ApiResponse(responseCode = "200", description = "Chi tiết công ty và hrAccounts")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có công ty")
    @ApiResponse(responseCode = "503", description = "EXTERNAL_UNAVAILABLE: không kết nối được identity-service")
    @GetMapping("/{id}")
    @PreAuthorize(Roles.STAFF)
    public StaffCompanyDetail detail(@PathVariable UUID id) {
        return staffCompanyService.detail(id);
    }

    @Operation(
            summary = "Khoá công ty",
            description = "Vai trò: STAFF. Lý do bắt buộc (trim, 1–1000 ký tự). Đặt công ty LOCKED, ghi lịch sử LOCK "
                    + "và khoá theo công ty mọi tài khoản HR ở identity-service (cùng thành công hoặc không đổi gì): "
                    + "HR không đăng nhập được, token còn hạn bị chặn 401 ACCOUNT_DISABLED; JD bị ẩn khỏi trang việc làm.")
    @ApiResponse(responseCode = "200", description = "Chi tiết công ty sau khi khoá, kèm lock {lockedAt, reason, lockedBy}")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: thiếu lý do hoặc quá 1000 ký tự (errors.reason)")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có công ty")
    @ApiResponse(responseCode = "409", description = "CONFLICT: công ty đã bị khoá")
    @ApiResponse(responseCode = "503",
            description = "EXTERNAL_UNAVAILABLE: không cập nhật được tài khoản HR ở identity-service, công ty không đổi")
    @PostMapping("/{id}/lock")
    @PreAuthorize(Roles.STAFF)
    public StaffCompanyDetail lock(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request,
                                   @AuthenticationPrincipal AuthPrincipal principal) {
        return companyLockService.lock(principal.getUserId(), id, request.reason());
    }

    @Operation(
            summary = "Mở khoá công ty",
            description = "Vai trò: STAFF. Không có body. Đặt công ty ACTIVE, ghi lịch sử UNLOCK và bỏ khoá theo công ty "
                    + "của mọi HR; HR đang bị khoá riêng vẫn bị khoá.")
    @ApiResponse(responseCode = "200", description = "Chi tiết công ty sau khi mở khoá (lock null)")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có công ty")
    @ApiResponse(responseCode = "409", description = "CONFLICT: công ty không bị khoá")
    @ApiResponse(responseCode = "503",
            description = "EXTERNAL_UNAVAILABLE: không cập nhật được tài khoản HR ở identity-service, công ty không đổi")
    @PostMapping("/{id}/unlock")
    @PreAuthorize(Roles.STAFF)
    public StaffCompanyDetail unlock(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return companyLockService.unlock(principal.getUserId(), id);
    }

    @Operation(
            summary = "Lịch sử thao tác trên công ty",
            description = "Vai trò: STAFF. Khoá/mở khoá công ty và duyệt/trả lại/đóng JD, mới nhất trước; page từ 1, "
                    + "size tối đa 100. staff.fullName null khi không tra được identity-service; job null với LOCK/UNLOCK.")
    @ApiResponse(responseCode = "200", description = "Trang lịch sử {items, total, page, size}")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: page/size không hợp lệ")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải STAFF")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có công ty")
    @GetMapping("/{id}/history")
    @PreAuthorize(Roles.STAFF)
    public PageResponse<CompanyActionItem> history(@PathVariable UUID id,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return companyHistoryService.history(id, page, size);
    }

    @PostMapping
    @PreAuthorize(Roles.STAFF)
    public ResponseEntity<CompanyResponse> create(@Valid @RequestBody CreateCompanyRequest request,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.status(201).body(companyService.create(request, principal.getUserId()));
    }

    @PostMapping("/{id}/hr-accounts")
    @PreAuthorize(Roles.STAFF)
    public ResponseEntity<HrAccountResponse> createHrAccount(@PathVariable UUID id,
                                                             @Valid @RequestBody CreateHrAccountRequest request) {
        return ResponseEntity.status(201).body(hrAccountService.create(id, request));
    }
}
