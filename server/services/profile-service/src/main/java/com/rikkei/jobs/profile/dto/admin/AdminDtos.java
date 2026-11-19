package com.rikkei.jobs.profile.dto.admin;

import com.rikkei.jobs.profile.entity.CandidateProfile.Gender;
import com.rikkei.jobs.profile.entity.EvidenceSource;
import com.rikkei.jobs.profile.entity.admin.ChangeEntity;
import com.rikkei.jobs.profile.entity.admin.DataReportStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Request/response của NT-12 (quản trị). Lý do sửa tối đa 500 ký tự, như lý do sửa tay của NT-05. */
public final class AdminDtos {
    private AdminDtos() {
    }

    // ---------- Kỹ năng ----------

    /** inUse = đã có bằng chứng, điểm hoặc ánh xạ nguồn: xoá sẽ chỉ tắt active. */
    public record SkillAdminView(UUID id, String code, String name, String group, boolean active, boolean inUse) { }

    public record CreateSkillRequest(
            @NotBlank @Size(max = 100) @Pattern(regexp = "[A-Za-z0-9_]+",
                    message = "Mã chỉ gồm chữ không dấu, số và dấu gạch dưới") String code,
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 100) String group,
            @Size(max = 500) String reason) { }

    /** Mã kỹ năng không đổi được (JD và ánh xạ nguồn tham chiếu theo id/mã). */
    public record UpdateSkillRequest(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 100) String group,
            @NotNull Boolean active,
            @Size(max = 500) String reason) { }

    public record DeleteSkillResult(UUID id, boolean deleted, boolean deactivated, String message) { }

    // ---------- Trọng số ----------

    public record ScoreWeightItem(@NotBlank String source,
                                  @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal weight) { }

    public record ScoreWeightsView(List<ScoreWeightItem> weights, BigDecimal total, Instant updatedAt) { }

    public record UpdateScoreWeightsRequest(@NotEmpty List<@Valid ScoreWeightItem> weights,
                                            @Size(max = 500) String reason) { }

    // ---------- Mẫu phiếu ----------

    public record CriterionView(UUID id, String name, String description, BigDecimal weight, int maxScore,
                                int sortOrder) { }

    public record TemplateView(UUID id, String name, String description, boolean active,
                               List<CriterionView> criteria, Instant updatedAt) { }

    public record CriterionRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("100") BigDecimal weight,
            @NotNull @Min(1) @Max(100) Integer maxScore) { }

    public record TemplateRequest(
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            Boolean active,
            @NotEmpty @Size(max = 50) List<@Valid CriterionRequest> criteria,
            @Size(max = 500) String reason) { }

    // ---------- Sửa hồ sơ, bằng chứng (bắt buộc lý do) ----------

    public record AdminProfileRequest(
            @NotBlank @Size(max = 255) String fullName,
            @NotBlank @Email @Size(max = 255) String email,
            @PastOrPresent LocalDate dob,
            Gender gender,
            @Pattern(regexp = "0[0-9]{9}") String phone,
            @Size(max = 500) String address,
            @Size(max = 255) String headline,
            @Size(max = 5000) String summary,
            @NotBlank(message = "Phải nhập lý do sửa") @Size(max = 500) String reason) { }

    /** score theo thang 0–100 như bằng chứng HS-01; bỏ trống trường nào thì giữ nguyên trường đó. */
    public record EvidenceUpdateRequest(
            @DecimalMin("0") @DecimalMax("100") BigDecimal score,
            @Size(max = 255) String title,
            @NotBlank(message = "Phải nhập lý do sửa") @Size(max = 500) String reason) { }

    public record EvidenceAdminView(UUID id, UUID userId, UUID skillId, String skillName, EvidenceSource source,
                                    String title, BigDecimal score, Instant recordedAt, boolean verified,
                                    boolean fromExternalRecord) { }

    // ---------- Báo dữ liệu sai ----------

    public record CreateDataReportRequest(@NotNull UUID evidenceId,
                                          @NotBlank @Size(max = 2000) String content) { }

    public record ResolveDataReportRequest(@NotNull DataReportStatus status,
                                           @NotBlank @Size(max = 2000) String resolution) { }

    public record DataReportView(UUID id, UUID userId, String candidateName, String candidateEmail, UUID evidenceId,
                                 String evidenceTitle, String skillName, String content, DataReportStatus status,
                                 String resolution, Instant resolvedAt, Instant createdAt,
                                 EvidenceAdminView evidence) { }

    // ---------- Nhật ký ----------

    public record ChangeLogView(UUID id, ChangeEntity entity, String entityId, String field, String oldValue,
                                String newValue, String reason, UUID adminId, Instant createdAt) { }
}
