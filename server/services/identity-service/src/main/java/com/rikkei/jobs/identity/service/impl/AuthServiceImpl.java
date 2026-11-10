package com.rikkei.jobs.identity.service.impl;

import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.identity.client.LmsClient;
import com.rikkei.jobs.identity.config.AppProperties;
import com.rikkei.jobs.identity.config.exception.ApiException;
import com.rikkei.jobs.identity.config.exception.ErrorCode;
import com.rikkei.jobs.identity.dto.request.ChangePasswordRequest;
import com.rikkei.jobs.identity.dto.request.LoginRequest;
import com.rikkei.jobs.identity.dto.request.SsoRequest;
import com.rikkei.jobs.identity.dto.response.LmsTicketInfo;
import com.rikkei.jobs.identity.dto.response.LoginResponse;
import com.rikkei.jobs.identity.dto.response.SsoResponse;
import com.rikkei.jobs.identity.dto.response.UserResponse;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.mapper.UserMapper;
import com.rikkei.jobs.identity.repository.UserRepository;
import com.rikkei.jobs.identity.service.AuthService;
import com.rikkei.jobs.identity.service.LoginService;
import com.rikkei.jobs.identity.service.SsoTicketService;
import com.rikkei.jobs.identity.service.SsoUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final LmsClient lmsClient;
    private final SsoTicketService ssoTicketService;
    private final SsoUserService ssoUserService;
    private final LoginService loginService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AppProperties properties;
    private final Clock clock;

    /**
     * Cố ý không @Transactional: việc đánh dấu vé đã dùng phải commit (hoặc lỗi) độc lập,
     * và lần đăng nhập SSO đầu tiên chạy song song được thử lại sau khi vi phạm khoá duy nhất.
     */
    @Override
    public SsoResponse sso(SsoRequest request) {
        LmsTicketInfo info = lmsClient.verify(request.getTicket())
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_TICKET));
        if (info.getRole() == null || !info.getRole().isLmsRole()) {
            throw new ApiException(ErrorCode.INVALID_TICKET);
        }

        Duration age = Duration.between(info.getIssuedAt(), clock.instant());
        if (age.compareTo(Duration.ofSeconds(properties.getAuth().getSsoTicketTtlSeconds())) > 0) {
            throw new ApiException(ErrorCode.TICKET_EXPIRED);
        }

        ssoTicketService.consume(request.getTicket());

        User user;
        try {
            user = ssoUserService.findOrCreate(info);
        } catch (DataIntegrityViolationException e) {
            user = ssoUserService.findOrCreate(info);
        }
        return SsoResponse.builder()
                .accessToken(issueToken(user))
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = loginService.authenticate(request.getEmail(), request.getPassword());
        return LoginResponse.builder()
                .accessToken(issueToken(user))
                .user(userMapper.toResponse(user))
                .mustChangePassword(user.getMustChangePassword())
                .build();
    }

    @Override
    @Transactional
    public String changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_OLD_PASSWORD);
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new ApiException(ErrorCode.SAME_PASSWORD);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        // Token cũ có thể mang claim mustChangePassword=true; trả token mới để các service khác chấp nhận ngay.
        return issueToken(user);
    }

    private String issueToken(User user) {
        return jwtUtils.generateJwtToken(user.getId(), user.getRole(), user.getCompanyId(),
                Boolean.TRUE.equals(user.getMustChangePassword()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));
        return userMapper.toResponse(user);
    }
}
