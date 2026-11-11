package com.rikkei.jobs.identity.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.rikkei.jobs.common.config.security.principle.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

/** Body của POST /internal/users (company-service gọi). Hiện chỉ tạo được tài khoản HR kèm companyId. */
@Data
public class InternalCreateUserRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    @Size(max = 255, message = "Email quá dài")
    private String email;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 255, message = "Họ tên quá dài")
    private String fullName;

    @NotNull(message = "Vai trò không được để trống")
    private Role role;

    private UUID companyId;

    /** Cắt khoảng trắng thừa trước khi validate để " a@b.c " vẫn hợp lệ. */
    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    @JsonIgnore
    @AssertTrue(message = "Chỉ tạo được tài khoản HR kèm companyId")
    public boolean isHrWithCompany() {
        return role == Role.HR && companyId != null;
    }
}
