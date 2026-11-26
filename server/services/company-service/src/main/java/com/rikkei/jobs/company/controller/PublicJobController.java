package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.company.dto.response.PublicCompanyDetail;
import com.rikkei.jobs.company.dto.response.PublicCompanyItem;
import com.rikkei.jobs.company.dto.response.PublicJobDetail;
import com.rikkei.jobs.company.dto.response.PublicJobItem;
import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import com.rikkei.jobs.company.service.PublicJobService;
import com.rikkei.jobs.company.service.PublicJobService.JobQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Qua gateway: {@code /api/v1/public/jobs/**}, {@code /api/v1/public/companies/**}. Không cần đăng nhập
 * (CompanySecurityConfig mở GET /public/**). Chỉ trả tin APPROVED, còn hạn, của công ty ACTIVE.
 */
@Tag(name = "Công khai - Việc làm")
@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
public class PublicJobController {

    private final PublicJobService publicJobService;

    @Operation(
            summary = "Tìm việc làm",
            description = "Không cần đăng nhập. q khớp tiêu đề, vị trí, tên công ty; hoặc JD có một trong skillIds "
                    + "(frontend đổi từ khoá thành id kỹ năng theo GET /skills). city khớp một phần, không phân biệt hoa "
                    + "thường. salaryFrom/salaryTo (VND) lọc khoảng lương giao nhau. sort: newest (mặc định) hoặc salary. "
                    + "page từ 1, size tối đa 100.")
    @ApiResponse(responseCode = "200", description = "Trang tin {items, total, page, size}")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: phân trang, enum hoặc sort không hợp lệ")
    @GetMapping("/jobs")
    public PageResponse<PublicJobItem> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<UUID> skillIds,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) JobLevel level,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) JobType type,
            @RequestParam(required = false) Long salaryFrom,
            @RequestParam(required = false) Long salaryTo,
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return publicJobService.search(new JobQuery(q, skillIds, city, level, workMode, type, salaryFrom, salaryTo,
                companyId, sort, page, size));
    }

    @Operation(summary = "Chi tiết việc làm", description = "Không cần đăng nhập. JD chưa duyệt, đã đóng, hết hạn "
            + "hoặc của công ty bị khoá trả 404.")
    @ApiResponse(responseCode = "200", description = "Chi tiết JD kèm công ty và kỹ năng")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có tin hoặc tin không còn hiển thị")
    @GetMapping("/jobs/{id}")
    public PublicJobDetail get(@PathVariable UUID id) {
        return publicJobService.get(id);
    }

    @Operation(summary = "Danh sách công ty", description = "Không cần đăng nhập. Công ty ACTIVE kèm số tin đang tuyển, "
            + "nhiều tin trước.")
    @ApiResponse(responseCode = "200", description = "Danh sách công ty")
    @GetMapping("/companies")
    public List<PublicCompanyItem> companies() {
        return publicJobService.companies();
    }

    @Operation(summary = "Hồ sơ công ty", description = "Không cần đăng nhập. Công ty bị khoá trả 404. "
            + "Tin đang tuyển lấy qua GET /public/jobs?companyId=.")
    @ApiResponse(responseCode = "200", description = "Hồ sơ công khai của công ty")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có công ty hoặc công ty bị khoá")
    @GetMapping("/companies/{id}")
    public PublicCompanyDetail company(@PathVariable UUID id) {
        return publicJobService.company(id);
    }
}
