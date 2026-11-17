package com.rikkei.jobs.profile.dto.sync;

import com.rikkei.jobs.profile.entity.MappingSource;
import com.rikkei.jobs.profile.entity.SyncStatus;
import com.rikkei.jobs.profile.entity.SyncTrigger;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Request/response của màn "Đồng bộ dữ liệu" (NT-05). */
public final class SyncDtos {
    private SyncDtos() {
    }

    @Schema(description = "Một lần đồng bộ")
    public record SyncRunView(
            UUID id,
            SyncTrigger trigger,
            @Schema(description = "Ứng viên; null khi job đêm lỗi trước khi tới ứng viên nào") UUID userId,
            String candidateName,
            String lmsUserId,
            SyncStatus status,
            Instant startedAt,
            Instant finishedAt,
            @Schema(description = "Lý do lỗi khi status = FAILED") String error,
            @Schema(description = "fetched, inserted, updated, unchanged, skipped; null khi lỗi")
            Map<String, Integer> counts,
            @Schema(description = "Cán bộ bấm đồng bộ; null với job đêm theo lịch và đồng bộ khi mở hồ sơ")
            UUID triggeredBy) { }

    public record CandidateSyncView(UUID userId, String fullName, String email, String lmsUserId,
                                    @Schema(description = "Lần đồng bộ gần nhất; null nếu chưa từng") SyncRunView lastRun) { }

    public record OverrideView(String field, String value, String reason, UUID staffId, Instant createdAt) { }

    public record ExternalRecordView(
            UUID id,
            MappingSource source,
            @Schema(description = "Khoá bên nguồn; null với bản ghi HS-01 nạp tay") String sourceKey,
            String sourceCode,
            String title,
            BigDecimal score,
            BigDecimal scoreMax,
            Instant recordedAt,
            Instant syncedAt,
            boolean verified,
            @Schema(description = "Bản ghi gốc nhận từ mock ở lần đồng bộ gần nhất") Map<String, Object> payload,
            @Schema(description = "Các trường đang giữ giá trị sửa tay (score, title)") List<String> overriddenFields,
            @Schema(description = "Lịch sử sửa tay, mới nhất trước") List<OverrideView> overrides) { }

    public record OverrideRequest(
            @Schema(description = "Trường cần sửa", allowableValues = {"score", "title"}, example = "score")
            @NotBlank(message = "Chọn trường cần sửa") String field,
            @Schema(description = "Giá trị mới; score theo thang của bản ghi (scoreMax)", example = "9")
            @NotBlank(message = "Nhập giá trị mới") @Size(max = 255, message = "Tối đa 255 ký tự") String value,
            @Schema(example = "Điểm phúc khảo theo quyết định số 12")
            @NotBlank(message = "Nhập lý do sửa") @Size(max = 500, message = "Tối đa 500 ký tự") String reason) { }

    public record NightlyStartedView(String message) { }

    public record PageView<T>(List<T> items, long total, int page, int size) { }
}
