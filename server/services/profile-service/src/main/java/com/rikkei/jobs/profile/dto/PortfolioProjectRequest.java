package com.rikkei.jobs.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** Dự án IOC chỉ nhận role, description, visible; các trường còn lại bị bỏ qua. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PortfolioProjectRequest(
    @NotBlank(message = "Vui lòng nhập tên dự án") @Size(max = 255, message = "Tên dự án tối đa 255 ký tự") String name,
    @Size(max = 255, message = "Vai trò tối đa 255 ký tự") String role,
    @Size(max = 5000, message = "Mô tả tối đa 5.000 ký tự") String description,
    @Size(max = 20, message = "Tối đa 20 công nghệ") List<
        @NotBlank(message = "Tên công nghệ không được rỗng") @Size(max = 50, message = "Tên công nghệ tối đa 50 ký tự")
        @Pattern(regexp = "[^,]+", message = "Tên công nghệ không được chứa dấu phẩy") String> techStack,
    @Size(max = 1000) @Pattern(regexp = URL, message = URL_MESSAGE) String repoUrl,
    @Size(max = 1000) @Pattern(regexp = URL, message = URL_MESSAGE) String demoUrl,
    LocalDate startDate, LocalDate endDate, Boolean visible) {
    static final String URL = "(?i)https?://[^\\s/?#]+([/?#]\\S*)?";
    static final String URL_MESSAGE = "Phải là URL http(s) hợp lệ";
    public PortfolioProjectRequest {
        name = trim(name); role = trim(role); description = trim(description);
        repoUrl = trim(repoUrl); demoUrl = trim(demoUrl);
        techStack = techStack == null ? List.of() : techStack.stream().map(t -> t == null ? null : t.strip()).distinct().toList();
    }
    @JsonIgnore @AssertTrue(message = "Ngày kết thúc phải bằng hoặc sau ngày bắt đầu")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
    private static String trim(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
