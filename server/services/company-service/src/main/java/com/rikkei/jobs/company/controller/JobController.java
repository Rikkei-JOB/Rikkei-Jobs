package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.company.dto.request.JobRequest;
import com.rikkei.jobs.company.dto.request.JobSkillRequest;
import com.rikkei.jobs.company.dto.response.JobResponse;
import com.rikkei.jobs.company.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Qua gateway: {@code /api/v1/jobs/**} (DN-02). Công ty chỉ lấy từ company_hr theo userId của JWT; body không
 * quyết định companyId/status. Phân quyền đặt ở từng method (không ở mức class) để DN-04/DN-07 nới GET /jobs/{id}.
 */
@Tag(name = "HR - JD")
@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @Operation(
            summary = "Tạo JD",
            description = "Vai trò: HR. Tạo JD nháp (DRAFT) cho công ty của HR. Chỉ title bắt buộc; skills tuỳ chọn, "
                    + "có skills thì kiểm danh mục kỹ năng của profile-service.")
    @ApiResponse(responseCode = "201", description = "JD vừa tạo")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: dữ liệu sai (errors theo field); BAD_REQUEST: JSON hỏng hoặc enum sai")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token; ACCOUNT_DISABLED: HR bị khoá")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải HR; COMPANY_LOCKED: công ty đang bị khoá")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: HR chưa được liên kết công ty")
    @ApiResponse(responseCode = "503", description = "EXTERNAL_UNAVAILABLE: không kiểm tra được danh mục kỹ năng")
    @PostMapping
    @PreAuthorize(Roles.HR)
    public ResponseEntity<JobResponse> create(
            @Valid @RequestBody JobRequest request,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(jobService.create(principal.getUserId(), request, authorization));
    }

    @Operation(
            summary = "Sửa JD",
            description = "Vai trò: HR. Thay toàn bộ thông tin chung (field không gửi bị xoá, trừ title). skills có mặt "
                    + "(kể cả mảng rỗng) thì thay luôn kỹ năng trong cùng transaction; vắng mặt thì giữ nguyên. "
                    + "Sửa JD APPROVED phải qua kiểm tra gửi duyệt và chuyển về PENDING.")
    @ApiResponse(responseCode = "200", description = "JD sau khi sửa")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: dữ liệu sai hoặc JD APPROVED không đủ điều kiện gửi duyệt; BAD_REQUEST: JSON hỏng, enum sai, id không phải UUID")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token; ACCOUNT_DISABLED: HR bị khoá")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải HR; COMPANY_LOCKED: công ty đang bị khoá")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD hoặc JD của công ty khác")
    @ApiResponse(responseCode = "409", description = "JOB_NOT_EDITABLE: JD đang PENDING hoặc CLOSED; CONFLICT: JD vừa được lưu ở nơi khác")
    @ApiResponse(responseCode = "503", description = "EXTERNAL_UNAVAILABLE: không kiểm tra được danh mục kỹ năng")
    @PutMapping("/{id}")
    @PreAuthorize(Roles.HR)
    public JobResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody JobRequest request,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return jobService.update(principal.getUserId(), id, request, authorization);
    }

    /** Body là mảng trần; quy tắc (tối đa 20, không trùng, minScore 1–100) kiểm ở service để trả 400 VALIDATION_ERROR. */
    @Operation(
            summary = "Thay kỹ năng yêu cầu của JD",
            description = "Vai trò: HR. Body [{skillId, required, minScore}] thay toàn bộ danh sách (tối đa 20, không trùng, "
                    + "minScore 1–100, mảng rỗng = xoá hết). Cùng quy tắc trạng thái như PUT /jobs/{id}.")
    @ApiResponse(responseCode = "200", description = "JD sau khi thay kỹ năng")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: errors.skills (sai cấu trúc, ngoài danh mục) hoặc JD APPROVED không đủ điều kiện gửi duyệt")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token; ACCOUNT_DISABLED: HR bị khoá")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải HR; COMPANY_LOCKED: công ty đang bị khoá")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD hoặc JD của công ty khác")
    @ApiResponse(responseCode = "409", description = "JOB_NOT_EDITABLE: JD đang PENDING hoặc CLOSED; CONFLICT: JD vừa được lưu ở nơi khác")
    @ApiResponse(responseCode = "503", description = "EXTERNAL_UNAVAILABLE: không kiểm tra được danh mục kỹ năng")
    @PutMapping("/{id}/skills")
    @PreAuthorize(Roles.HR)
    public JobResponse replaceSkills(
            @PathVariable UUID id,
            @RequestBody List<JobSkillRequest> skills,
            @Parameter(hidden = true) @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return jobService.replaceSkills(principal.getUserId(), id, skills, authorization);
    }

    @Operation(
            summary = "Gửi duyệt JD",
            description = "Vai trò: HR. Chỉ từ DRAFT hoặc REJECTED (xoá lý do trả lại); kiểm đủ trường, hạn nộp, "
                    + "lương và ít nhất một kỹ năng bắt buộc, errors liệt kê mọi điều kiện sai. Không có body.")
    @ApiResponse(responseCode = "200", description = "JD đã chuyển PENDING")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: thiếu hoặc sai điều kiện gửi duyệt (errors theo field); BAD_REQUEST: id không phải UUID")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token; ACCOUNT_DISABLED: HR bị khoá")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải HR; COMPANY_LOCKED: công ty đang bị khoá")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD hoặc JD của công ty khác")
    @ApiResponse(responseCode = "409", description = "INVALID_JOB_STATUS: JD không ở DRAFT/REJECTED; CONFLICT: JD vừa được lưu ở nơi khác")
    @PostMapping("/{id}/submit")
    @PreAuthorize(Roles.HR)
    public JobResponse submit(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return jobService.submit(principal.getUserId(), id);
    }

    @Operation(
            summary = "Đóng JD",
            description = "Vai trò: HR. Chỉ đóng JD đang APPROVED. Không có body.")
    @ApiResponse(responseCode = "200", description = "JD đã chuyển CLOSED")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: id không phải UUID")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token; ACCOUNT_DISABLED: HR bị khoá")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải HR; COMPANY_LOCKED: công ty đang bị khoá")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD hoặc JD của công ty khác")
    @ApiResponse(responseCode = "409", description = "INVALID_JOB_STATUS: JD không ở APPROVED; CONFLICT: JD vừa được lưu ở nơi khác")
    @PostMapping("/{id}/close")
    @PreAuthorize(Roles.HR)
    public JobResponse close(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return jobService.close(principal.getUserId(), id);
    }

    @Operation(
            summary = "Xem JD",
            description = "Vai trò: HR (DN-02). Chỉ JD của công ty mình; công ty LOCKED vẫn xem được.")
    @ApiResponse(responseCode = "200", description = "Chi tiết JD kèm kỹ năng")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: id không phải UUID")
    @ApiResponse(responseCode = "401", description = "UNAUTHORIZED: thiếu hoặc sai token; ACCOUNT_DISABLED: HR bị khoá")
    @ApiResponse(responseCode = "403", description = "FORBIDDEN: không phải HR")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD hoặc JD của công ty khác")
    @GetMapping("/{id}")
    @PreAuthorize(Roles.HR)
    public JobResponse get(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return jobService.get(principal.getUserId(), id);
    }
}
