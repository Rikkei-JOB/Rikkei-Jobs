package com.rikkei.jobs.company.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** DN-07: lý do trả lại JD; HR thấy nguyên văn ở trang JD của mình. */
public record RejectJobRequest(
        @NotBlank(message = "Vui lòng nhập lý do trả lại.")
        @Size(max = 1000, message = "Lý do tối đa 1.000 ký tự.") String reason) {

    public RejectJobRequest {
        reason = reason == null ? null : reason.strip();
    }
}
