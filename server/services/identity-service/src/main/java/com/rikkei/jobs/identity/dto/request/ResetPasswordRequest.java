package com.rikkei.jobs.identity.dto.request;

import com.rikkei.jobs.identity.util.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Token không được để trống")
    @Size(max = 255, message = "Token quá dài")
    private String token;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(max = 128, message = "Mật khẩu quá dài")
    @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
    private String newPassword;
}
