package com.rikkei.jobs.profile.dto;

import com.rikkei.jobs.profile.entity.CvSection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** HS-04: request/response của CV và trang hồ sơ công khai qua QR. */
public final class CvDtos {
    private CvDtos() { }

    public record CvRequest(@NotBlank @Size(max = 255) String name, @Size(max = 255) String targetPosition) { }

    /** Item khớp hợp đồng UT-01 {id, name, targetPosition}. */
    public record CvSummary(UUID id, String name, String targetPosition, String template, String publicUrl,
        Instant createdAt, Instant updatedAt) { }

    public record CvDetail(UUID id, String name, String targetPosition, String template, String publicUrl,
        Instant createdAt, Instant updatedAt, List<SectionView> sections) { }

    /** allItems = true: CV tự lấy mọi mục, kể cả mục thêm vào hồ sơ sau này. */
    public record SectionView(CvSection.Type section, String label, boolean included, boolean allItems,
        String text, List<ItemView> items) { }

    /** verified = null khi mục không có khái niệm xác thực (học vấn, chứng chỉ). */
    public record ItemView(UUID id, String title, String subtitle, String period, String detail,
        Boolean verified, boolean selected) { }

    public record SectionsRequest(@NotNull @Size(min = 1, max = 20) List<@NotNull @Valid SectionRequest> sections) { }

    /** itemIds = null: lấy mọi mục. Thứ tự trong danh sách là thứ tự phần trên CV. */
    public record SectionRequest(@NotNull CvSection.Type section, boolean included,
        @Size(max = 150) List<@NotNull UUID> itemIds) { }

    /** Trang /p/{token}: chỉ dữ liệu đã xác thực, không có email/điện thoại. */
    public record PublicProfile(boolean visible, String fullName, String headline, String program,
        String courseName, List<PublicSkill> skills, List<PublicInternship> internships) {
        public static PublicProfile hidden() { return new PublicProfile(false, null, null, null, null, List.of(), List.of()); }
    }

    public record PublicSkill(String name, String group, String level, BigDecimal verifiedScore) { }

    public record PublicInternship(String name, String role, List<String> techStack, String period, String description) { }
}
