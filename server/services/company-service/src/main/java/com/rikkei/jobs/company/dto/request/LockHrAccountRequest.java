package com.rikkei.jobs.company.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LockHrAccountRequest(
        @NotBlank(message = "Lý do khoá không được để trống")
        @Size(max = 500, message = "Lý do khoá tối đa 500 ký tự")
        String reason) {
}
