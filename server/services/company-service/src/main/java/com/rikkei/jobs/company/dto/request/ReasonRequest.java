package com.rikkei.jobs.company.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Lý do của cán bộ khi trả lại / đóng JD hoặc khoá công ty; service lưu {@code reason.strip()}. */
public record ReasonRequest(
        @NotBlank(message = "Vui lòng nhập lý do")
        @Size(max = 1000, message = "Lý do tối đa 1000 ký tự")
        String reason) {
}
