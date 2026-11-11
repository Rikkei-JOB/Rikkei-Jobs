package com.rikkei.jobs.identity.service;

import com.rikkei.jobs.identity.dto.request.ChangePasswordRequest;
import com.rikkei.jobs.identity.dto.request.LoginRequest;
import com.rikkei.jobs.identity.dto.request.SsoRequest;
import com.rikkei.jobs.identity.dto.response.LoginResponse;
import com.rikkei.jobs.identity.dto.response.SsoResponse;
import com.rikkei.jobs.identity.dto.response.UserResponse;

import java.util.UUID;

public interface AuthService {
    SsoResponse sso(SsoRequest request);

    LoginResponse login(LoginRequest request);

    String changePassword(UUID userId, ChangePasswordRequest request);

    UserResponse getCurrentUser(UUID userId);
}
