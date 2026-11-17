package com.rikkei.jobs.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Body của PATCH /internal/users/{id}/lock: lý do khoá của cán bộ. */
@Data
public class InternalLockUserRequest {
    @NotBlank(message = "Lý do khoá không được để trống")
    @Size(max = 500, message = "Lý do khoá tối đa 500 ký tự")
    private String reason;

    /** Cắt khoảng trắng thừa trước khi validate để chuỗi toàn dấu cách bị coi là rỗng. */
    public void setReason(String reason) {
        this.reason = reason == null ? null : reason.trim();
    }
}
