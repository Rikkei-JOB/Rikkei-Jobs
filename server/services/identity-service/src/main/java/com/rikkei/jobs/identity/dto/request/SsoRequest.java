package com.rikkei.jobs.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SsoRequest {
    @NotBlank(message = "Vé SSO không được để trống")
    @Size(max = 255, message = "Vé SSO quá dài")
    private String ticket;
}
