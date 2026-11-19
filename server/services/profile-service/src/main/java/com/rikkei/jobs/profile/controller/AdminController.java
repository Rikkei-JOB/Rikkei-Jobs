package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.CandidateProfileResponse;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.AdminProfileRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.ChangeLogView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.CreateSkillRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.DataReportView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.DeleteSkillResult;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.EvidenceAdminView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.EvidenceUpdateRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.ResolveDataReportRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.ScoreWeightsView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.SkillAdminView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.TemplateRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.TemplateView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.UpdateScoreWeightsRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.UpdateSkillRequest;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.PageView;
import com.rikkei.jobs.profile.entity.admin.ChangeEntity;
import com.rikkei.jobs.profile.entity.admin.DataReportStatus;
import com.rikkei.jobs.profile.exception.FieldValidationException;
import com.rikkei.jobs.profile.service.admin.AdminRecordService;
import com.rikkei.jobs.profile.service.admin.AdminSkillService;
import com.rikkei.jobs.profile.service.admin.ChangeLogService;
import com.rikkei.jobs.profile.service.admin.DataReportService;
import com.rikkei.jobs.profile.service.admin.EvaluationTemplateService;
import com.rikkei.jobs.profile.service.admin.ScoreWeightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * NT-12: trang Quản trị. Chỉ ADMIN; vai trò khác nhận 403. Qua gateway: /api/v1/admin/...
 * Mọi thay đổi ghi change_logs; sửa hồ sơ và bằng chứng bắt buộc có lý do.
 */
@Tag(name = "Quản trị (NT-12)")
@RestController
@RequestMapping("/admin")
@PreAuthorize(Roles.ADMIN)
@RequiredArgsConstructor
public class AdminController {
    private final AdminSkillService skills;
    private final ScoreWeightService weights;
    private final EvaluationTemplateService templates;
    private final AdminRecordService records;
    private final DataReportService reports;
    private final ChangeLogService changeLogs;

    // ---------- Kỹ năng ----------

    @Operation(summary = "Danh mục kỹ năng, gồm cả kỹ năng đã tắt")
    @GetMapping("/skills")
    public List<SkillAdminView> listSkills() {
        return skills.list();
    }

    @Operation(summary = "Thêm kỹ năng", description = "Mã viết hoa, không trùng. Kỹ năng mới hiện ngay ở form JD và hồ sơ.")
    @PostMapping("/skills")
    @ResponseStatus(HttpStatus.CREATED)
    public SkillAdminView createSkill(@Valid @RequestBody CreateSkillRequest request,
                                      @AuthenticationPrincipal AuthPrincipal principal) {
        return skills.create(request, principal.getUserId());
    }

    @Operation(summary = "Sửa tên, nhóm, bật/tắt kỹ năng")
    @PutMapping("/skills/{id}")
    public SkillAdminView updateSkill(@PathVariable UUID id, @Valid @RequestBody UpdateSkillRequest request,
                                      @AuthenticationPrincipal AuthPrincipal principal) {
        return skills.update(id, request, principal.getUserId());
    }

    @Operation(summary = "Xoá kỹ năng",
            description = "Kỹ năng đã có bằng chứng, điểm hoặc ánh xạ nguồn chỉ bị tắt (deactivated=true), dữ liệu cũ giữ nguyên.")
    @DeleteMapping("/skills/{id}")
    public DeleteSkillResult deleteSkill(@PathVariable UUID id,
                                         @RequestParam(required = false) String reason,
                                         @AuthenticationPrincipal AuthPrincipal principal) {
        return skills.delete(id, checkReason(reason), principal.getUserId());
    }

    // ---------- Trọng số ----------

    @Operation(summary = "Trọng số nguồn điểm LMS, EXAM, INTERNSHIP, MENTOR")
    @GetMapping("/score-weights")
    public ScoreWeightsView getWeights() {
        return weights.get();
    }

    @Operation(summary = "Lưu trọng số", description = "Đủ 4 nguồn, tổng phải bằng 100; sai trả 400 VALIDATION_ERROR.")
    @PutMapping("/score-weights")
    public ScoreWeightsView updateWeights(@Valid @RequestBody UpdateScoreWeightsRequest request,
                                          @AuthenticationPrincipal AuthPrincipal principal) {
        return weights.update(request, principal.getUserId());
    }

    // ---------- Mẫu phiếu ----------

    @Operation(summary = "Danh sách mẫu phiếu đánh giá kèm tiêu chí")
    @GetMapping("/evaluation-templates")
    public List<TemplateView> listTemplates() {
        return templates.list(false);
    }

    @GetMapping("/evaluation-templates/{id}")
    public TemplateView getTemplate(@PathVariable UUID id) {
        return templates.get(id);
    }

    @PostMapping("/evaluation-templates")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateView createTemplate(@Valid @RequestBody TemplateRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return templates.create(request, principal.getUserId());
    }

    @Operation(summary = "Sửa mẫu phiếu", description = "Danh sách tiêu chí trong request thay toàn bộ tiêu chí cũ.")
    @PutMapping("/evaluation-templates/{id}")
    public TemplateView updateTemplate(@PathVariable UUID id, @Valid @RequestBody TemplateRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return templates.update(id, request, principal.getUserId());
    }

    @DeleteMapping("/evaluation-templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable UUID id,
                               @RequestParam(required = false) String reason,
                               @AuthenticationPrincipal AuthPrincipal principal) {
        templates.delete(id, checkReason(reason), principal.getUserId());
    }

    // ---------- Hồ sơ, bằng chứng ----------

    @GetMapping("/profiles/{userId}")
    public CandidateProfileResponse getProfile(@PathVariable UUID userId) {
        return records.getProfile(userId);
    }

    @Operation(summary = "Sửa hồ sơ ứng viên", description = "Bắt buộc reason; mỗi trường đổi ghi change_logs.")
    @PutMapping("/profiles/{userId}")
    public CandidateProfileResponse updateProfile(@PathVariable UUID userId,
                                                  @Valid @RequestBody AdminProfileRequest request,
                                                  @AuthenticationPrincipal AuthPrincipal principal) {
        return records.updateProfile(userId, request, principal.getUserId());
    }

    @Operation(summary = "Bằng chứng kỹ năng của một ứng viên")
    @GetMapping("/evidences")
    public List<EvidenceAdminView> listEvidences(@RequestParam UUID userId) {
        return records.listEvidences(userId);
    }

    @Operation(summary = "Sửa bằng chứng",
            description = "Bắt buộc reason. score thang 0–100. Bằng chứng từ nguồn ngoài được sửa ở bản ghi gốc (giữ qua các lần đồng bộ).")
    @PutMapping("/evidences/{id}")
    public EvidenceAdminView updateEvidence(@PathVariable UUID id, @Valid @RequestBody EvidenceUpdateRequest request,
                                            @AuthenticationPrincipal AuthPrincipal principal) {
        return records.updateEvidence(id, request, principal.getUserId());
    }

    // ---------- Báo dữ liệu sai ----------

    @GetMapping("/data-reports")
    public PageView<DataReportView> listReports(@RequestParam(required = false) DataReportStatus status,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return reports.list(status, page, size);
    }

    @Operation(summary = "Xử lý báo cáo", description = "status RESOLVED hoặc REJECTED, resolution là kết quả ứng viên sẽ thấy.")
    @PutMapping("/data-reports/{id}")
    public DataReportView resolveReport(@PathVariable UUID id, @Valid @RequestBody ResolveDataReportRequest request,
                                        @AuthenticationPrincipal AuthPrincipal principal) {
        return reports.resolve(id, request, principal.getUserId());
    }

    // ---------- Nhật ký ----------

    @Operation(summary = "Lịch sử thay đổi", description = "from, to dạng yyyy-MM-dd theo giờ Việt Nam, tính cả ngày to.")
    @GetMapping("/change-logs")
    public PageView<ChangeLogView> changeLogs(@RequestParam(required = false) ChangeEntity entity,
                                              @RequestParam(required = false) String entityId,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return changeLogs.search(entity, entityId, from, to, page, size);
    }

    private static String checkReason(String reason) {
        if (reason != null && reason.length() > 500) {
            throw new FieldValidationException("reason", "Lý do tối đa 500 ký tự");
        }
        return reason;
    }
}
