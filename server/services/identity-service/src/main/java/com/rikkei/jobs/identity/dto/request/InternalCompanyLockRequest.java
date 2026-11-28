package com.rikkei.jobs.identity.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Body của POST /internal/users/company-lock: khoá / mở khoá theo công ty cho nhiều HR một lần. */
@Data
public class InternalCompanyLockRequest {
    @NotEmpty(message = "Danh sách userIds không được để trống")
    @Size(max = 100, message = "Tối đa 100 userIds mỗi lần")
    private List<@NotNull(message = "userId không được null") UUID> userIds;

    @NotNull(message = "locked là bắt buộc")
    private Boolean locked;
}
