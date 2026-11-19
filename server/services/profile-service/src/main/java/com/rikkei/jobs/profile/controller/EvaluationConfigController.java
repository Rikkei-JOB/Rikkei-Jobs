package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.profile.dto.admin.AdminDtos.ScoreWeightsView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.TemplateView;
import com.rikkei.jobs.profile.service.admin.EvaluationTemplateService;
import com.rikkei.jobs.profile.service.admin.ScoreWeightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cấu hình do ADMIN đặt ở NT-12, mở cho các task đánh giá đọc: DG-03 (trọng số) và DG-01 (mẫu phiếu đang dùng).
 * Service khác gọi qua API này, không đọc thẳng bảng của profile-service.
 */
@Tag(name = "Cấu hình đánh giá (NT-12)")
@RestController
@PreAuthorize("hasAnyRole('MENTOR','STAFF','ADMIN')")
@RequiredArgsConstructor
public class EvaluationConfigController {
    private final ScoreWeightService weights;
    private final EvaluationTemplateService templates;

    @Operation(summary = "Trọng số nguồn điểm hiện hành (DG-03)")
    @GetMapping("/score-weights")
    public ScoreWeightsView weights() {
        return weights.get();
    }

    @Operation(summary = "Mẫu phiếu đánh giá đang bật (DG-01)")
    @GetMapping("/evaluation-templates")
    public List<TemplateView> activeTemplates() {
        return templates.list(true);
    }
}
