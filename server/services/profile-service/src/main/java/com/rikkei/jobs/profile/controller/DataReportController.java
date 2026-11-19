package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.CreateDataReportRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.DataReportView;
import com.rikkei.jobs.profile.service.admin.DataReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** NT-12: ứng viên báo bằng chứng sai và xem kết quả xử lý. Qua gateway: /api/v1/me/data-reports. */
@Tag(name = "Báo dữ liệu sai (NT-12)")
@RestController
@RequestMapping("/me/data-reports")
@PreAuthorize(Roles.CANDIDATE)
@RequiredArgsConstructor
public class DataReportController {
    private final DataReportService reports;

    @Operation(summary = "Báo một bằng chứng sai",
            description = "Bằng chứng phải của chính ứng viên (khác thì 404). Mỗi bằng chứng chỉ có một báo cáo đang chờ (409).")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataReportView create(@Valid @RequestBody CreateDataReportRequest request,
                                 @AuthenticationPrincipal AuthPrincipal principal) {
        return reports.create(principal.getUserId(), request);
    }

    @Operation(summary = "Các báo cáo của tôi kèm kết quả xử lý")
    @GetMapping
    public List<DataReportView> mine(@AuthenticationPrincipal AuthPrincipal principal) {
        return reports.mine(principal.getUserId());
    }
}
