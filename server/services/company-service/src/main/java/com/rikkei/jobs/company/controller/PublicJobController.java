package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.response.PublicJobDetail;
import com.rikkei.jobs.company.dto.response.PublicJobSummary;
import com.rikkei.jobs.company.service.PublicJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Qua gateway: {@code GET /api/v1/jobs} và {@code GET /api/v1/jobs/{id}} (DN-07 §4.3). Công khai
 * (CompanySecurityConfig), nên không có @PreAuthorize; token sai hoặc thiếu vẫn xem được.
 */
@Tag(name = "Việc làm")
@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class PublicJobController {

    private final PublicJobService publicJobService;

    @Operation(
            summary = "Danh sách việc làm",
            description = "Công khai (không cần đăng nhập). Chỉ JD đã duyệt, còn hạn nộp (tính theo giờ Việt Nam) "
                    + "của công ty đang hoạt động; mới duyệt trước. page từ 1, size tối đa 100.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Trang JD {items, total, page, size}")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: page/size không hợp lệ")
    @GetMapping
    public PageResponse<PublicJobSummary> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return publicJobService.list(page, size);
    }

    @Operation(
            summary = "Chi tiết việc làm",
            description = "Công khai (không cần đăng nhập). JD chờ duyệt, nháp, đã đóng, quá hạn, của công ty bị khoá "
                    + "hoặc không tồn tại đều trả cùng một 404. Kỹ năng chỉ có skillId (ghép tên với GET /skills).")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Chi tiết JD kèm kỹ năng và thông tin công ty")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: id không phải UUID")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: JD không hiện với ứng viên hoặc không tồn tại")
    @GetMapping("/{id}")
    public PublicJobDetail get(@PathVariable UUID id) {
        return publicJobService.get(id);
    }
}
