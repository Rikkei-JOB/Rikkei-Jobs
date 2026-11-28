package com.rikkei.jobs.company.dto.request;

import com.rikkei.jobs.company.util.ReasonRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Lý do trả lại JD; HR thấy nguyên văn ở trang JD của mình. Thông điệp dùng chung với
 * {@link ReasonRules} để lý do trả lại / đóng JD / khoá công ty báo lỗi giống nhau (DN-07).
 */
public record RejectJobRequest(
        @NotBlank(message = ReasonRules.REQUIRED_MESSAGE)
        @Size(max = ReasonRules.MAX_LENGTH, message = ReasonRules.TOO_LONG_MESSAGE) String reason) {

    public RejectJobRequest {
        reason = reason == null ? null : reason.strip();
    }
}
