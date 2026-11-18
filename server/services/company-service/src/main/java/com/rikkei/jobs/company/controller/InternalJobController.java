package com.rikkei.jobs.company.controller;

import com.rikkei.jobs.company.dto.response.JobApplicationContextResponse;
import com.rikkei.jobs.company.service.JobContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API nội bộ cho recruitment-service (UT-01); gateway không định tuyến, bảo vệ bằng X-Internal-Key. */
@Tag(name = "Nội bộ")
@RestController
@RequestMapping("/internal/jobs")
@RequiredArgsConstructor
public class InternalJobController {

    private final JobContextService jobContextService;

    @Operation(
            summary = "Ngữ cảnh JD cho nộp hồ sơ",
            description = "Vai trò: dịch vụ nội bộ (header X-Internal-Key), không dùng JWT. "
                    + "deadline là 00:00 ngày kế tiếp theo giờ Việt Nam; hrEmails chỉ gồm HR chưa bị khoá.")
    @ApiResponse(responseCode = "200", description = "Ngữ cảnh JD")
    @ApiResponse(responseCode = "400", description = "BAD_REQUEST: jobId không phải UUID")
    @ApiResponse(responseCode = "401", description = "INVALID_SERVICE_KEY: thiếu hoặc sai khoá dịch vụ nội bộ")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có JD")
    @ApiResponse(responseCode = "503", description = "EXTERNAL_UNAVAILABLE: identity-service không trả lời")
    @GetMapping("/{jobId}/application-context")
    public JobApplicationContextResponse applicationContext(@PathVariable UUID jobId) {
        return jobContextService.applicationContext(jobId);
    }
}
