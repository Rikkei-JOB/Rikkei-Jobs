package com.rikkei.jobs.identity.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Body của POST /internal/users/lookup: tra cứu hàng loạt trạng thái tài khoản. */
@Data
public class InternalUserLookupRequest {
    @NotEmpty(message = "Danh sách userIds không được để trống")
    @Size(max = 100, message = "Tối đa 100 userIds mỗi lần tra cứu")
    private List<UUID> userIds;
}
