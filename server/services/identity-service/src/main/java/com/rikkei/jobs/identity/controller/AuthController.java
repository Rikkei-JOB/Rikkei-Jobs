package com.rikkei.jobs.identity.controller;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.identity.dto.request.ChangePasswordRequest;
import com.rikkei.jobs.identity.dto.request.ForgotPasswordRequest;
import com.rikkei.jobs.identity.dto.request.LoginRequest;
import com.rikkei.jobs.identity.dto.request.ResetPasswordRequest;
import com.rikkei.jobs.identity.dto.request.SsoRequest;
import com.rikkei.jobs.identity.dto.response.ChangePasswordResponse;
import com.rikkei.jobs.identity.dto.response.LoginResponse;
import com.rikkei.jobs.identity.dto.response.MessageResponse;
import com.rikkei.jobs.identity.dto.response.SsoResponse;
import com.rikkei.jobs.identity.dto.response.UserResponse;
import com.rikkei.jobs.identity.service.AuthService;
import com.rikkei.jobs.identity.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Qua gateway: /api/v1/auth/... (gateway bỏ tiền tố /api/v1, service nhận /auth/...). */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/sso")
    public SsoResponse sso(@Valid @RequestBody SsoRequest request) {
        return authService.sso(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ChangePasswordResponse changePassword(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        String accessToken = authService.changePassword(principal.getUserId(), request);
        return new ChangePasswordResponse("Đổi mật khẩu thành công", accessToken);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request);
        return new MessageResponse("Nếu email tồn tại trong hệ thống, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.reset(request);
        return new MessageResponse("Đặt lại mật khẩu thành công");
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public UserResponse getCurrentUser(@AuthenticationPrincipal AuthPrincipal principal) {
        return authService.getCurrentUser(principal.getUserId());
    }
}
