package com.rikkei.jobs.company.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Lý do của cán bộ khi trả lại / đóng JD hoặc khoá công ty. Không dùng {@code @Size}: giới hạn 1000 ký tự áp cho
 * lý do đã strip, kiểm ở {@link com.rikkei.jobs.company.util.ReasonRules#clean}.
 */
public record ReasonRequest(
        @NotBlank(message = "Vui lòng nhập lý do")
        String reason) {
}
