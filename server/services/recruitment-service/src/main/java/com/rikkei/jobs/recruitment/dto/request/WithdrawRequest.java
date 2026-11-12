package com.rikkei.jobs.recruitment.dto.request;
import jakarta.validation.constraints.*;
public record WithdrawRequest(@NotBlank(message="Vui lòng nhập lý do rút đơn.")
    @Size(max=1000, message="Lý do rút đơn tối đa 1.000 ký tự.") String reason) {
    public WithdrawRequest { if (reason != null) reason = reason.strip(); }
}
