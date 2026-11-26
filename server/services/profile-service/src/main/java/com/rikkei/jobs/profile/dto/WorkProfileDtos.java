package com.rikkei.jobs.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Hồ sơ nghề nghiệp của Mentor / Staff / Admin: GET/PUT /profiles/work/me. */
public final class WorkProfileDtos {
    private WorkProfileDtos() { }

    /** updatedAt null nghĩa là chưa lưu hồ sơ lần nào (mọi trường trống). */
    public record WorkProfileResponse(String title, String organization, Integer yearsExperience, String phone,
                                      String linkedinUrl, List<String> expertise, String bio, Instant updatedAt) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UpdateWorkProfileRequest(
        @Size(max = 255, message = "Chức danh tối đa 255 ký tự.") String title,
        @Size(max = 255, message = "Đơn vị công tác tối đa 255 ký tự.") String organization,
        @Min(value = 0, message = "Số năm kinh nghiệm không được âm.")
        @Max(value = 60, message = "Số năm kinh nghiệm tối đa 60.") Integer yearsExperience,
        @Pattern(regexp = "0[0-9]{9}", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0.") String phone,
        @Size(max = 500, message = "Liên kết tối đa 500 ký tự.")
        @Pattern(regexp = "https?://\\S+", message = "Liên kết phải bắt đầu bằng http:// hoặc https://.") String linkedinUrl,
        @Size(max = 12, message = "Tối đa 12 lĩnh vực chuyên môn.")
        List<@NotBlank @Size(max = 50, message = "Mỗi lĩnh vực tối đa 50 ký tự.") String> expertise,
        @Size(max = 3000, message = "Giới thiệu tối đa 3.000 ký tự.") String bio
    ) {
        public UpdateWorkProfileRequest {
            title = trim(title); organization = trim(organization); phone = trim(phone);
            linkedinUrl = trim(linkedinUrl); bio = trim(bio);
            // Bỏ mục trống và trùng (không phân biệt hoa thường), giữ thứ tự người dùng nhập.
            if (expertise != null) {
                LinkedHashMap<String, String> unique = new LinkedHashMap<>();
                for (String item : expertise) {
                    String value = trim(item);
                    if (value != null) unique.putIfAbsent(value.toLowerCase(Locale.ROOT), value);
                }
                expertise = List.copyOf(unique.values());
            }
        }

        private static String trim(String value) {
            return value == null || value.isBlank() ? null : value.strip();
        }
    }
}
