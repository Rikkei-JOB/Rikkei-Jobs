package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.CandidateSyncView;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.ExternalRecordView;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.NightlyStartedView;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.OverrideRequest;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.PageView;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.SyncRunView;
import com.rikkei.jobs.profile.entity.SyncStatus;
import com.rikkei.jobs.profile.entity.SyncTrigger;
import com.rikkei.jobs.profile.service.sync.SyncAdminService;
import com.rikkei.jobs.profile.service.sync.SyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** NT-05: cán bộ xem nhật ký đồng bộ, đồng bộ lại một ứng viên, xem và sửa tay bản ghi nguồn. */
@Tag(name = "NT-05 Đồng bộ dữ liệu", description = "Dữ liệu học tập, thi, thực tập từ mock LMS / Khảo thí / lms-pm")
@RestController
@RequiredArgsConstructor
@PreAuthorize(Roles.STAFF)
public class StaffSyncController {
    private final SyncService sync;
    private final SyncAdminService admin;

    @Operation(summary = "Đồng bộ lại một ứng viên",
            description = "Gọi 3 API mock, upsert external_records theo source_key, ghi sync_runs rồi tính lại kỹ năng "
                    + "HS-01. Mock lỗi thì vẫn trả 200 với status = FAILED và error; dữ liệu cũ giữ nguyên.")
    @PostMapping("/staff/sync/{userId}")
    public SyncRunView syncUser(@PathVariable UUID userId, @AuthenticationPrincipal AuthPrincipal principal) {
        return admin.view(sync.syncNow(userId, SyncTrigger.MANUAL, principal.getUserId()));
    }

    @Operation(summary = "Chạy tay job đêm",
            description = "Đồng bộ mọi ứng viên đã liên kết LMS ở luồng nền (giống job 01:00). Trả 202 ngay; "
                    + "kết quả xem ở GET /staff/sync-runs. Đang chạy thì 409.")
    @PostMapping("/staff/sync/nightly")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public NightlyStartedView runNightly(@AuthenticationPrincipal AuthPrincipal principal) {
        sync.startNightlyInBackground(principal.getUserId());
        return new NightlyStartedView("Đã bắt đầu đồng bộ toàn bộ ứng viên");
    }

    @Operation(summary = "Tìm ứng viên để đồng bộ", description = "Theo tên, email hoặc mã LMS; tối đa 20 kết quả.")
    @GetMapping("/staff/sync/candidates")
    public List<CandidateSyncView> candidates(@RequestParam(required = false) String q) {
        return admin.searchCandidates(q);
    }

    @Operation(summary = "Danh sách lần đồng bộ", description = "Mới nhất trước; lọc theo trạng thái và ứng viên.")
    @GetMapping("/staff/sync-runs")
    public PageView<SyncRunView> runs(@RequestParam(required = false) SyncStatus status,
                                      @RequestParam(required = false) UUID userId,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "20") int size) {
        return admin.listRuns(status, userId, page, size);
    }

    @Operation(summary = "Bản ghi nguồn của một ứng viên", description = "Kèm payload gốc và lịch sử sửa tay.")
    @GetMapping("/staff/external-records")
    public List<ExternalRecordView> records(@RequestParam UUID userId) {
        return admin.listRecords(userId);
    }

    @Operation(summary = "Sửa tay một trường của bản ghi",
            description = "field là score hoặc title; bắt buộc có lý do. Các lần đồng bộ sau không ghi đè trường này.")
    @PutMapping("/staff/external-records/{id}/override")
    public ExternalRecordView override(@PathVariable UUID id, @Valid @RequestBody OverrideRequest request,
                                       @AuthenticationPrincipal AuthPrincipal principal) {
        return admin.override(id, request, principal.getUserId());
    }
}
