package com.rikkei.jobs.identity.service.impl;

import com.rikkei.jobs.identity.config.AppProperties;
import com.rikkei.jobs.identity.config.exception.ApiException;
import com.rikkei.jobs.identity.config.exception.ErrorCode;
import com.rikkei.jobs.identity.dto.request.ForgotPasswordRequest;
import com.rikkei.jobs.identity.dto.request.ResetPasswordRequest;
import com.rikkei.jobs.identity.entity.PasswordResetToken;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.entity.UserStatus;
import com.rikkei.jobs.identity.repository.PasswordResetTokenRepository;
import com.rikkei.jobs.identity.repository.UserRepository;
import com.rikkei.jobs.identity.service.MailService;
import com.rikkei.jobs.identity.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AppProperties properties;
    private final Clock clock;

    @Override
    public void requestReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase()).orElse(null);
        // Chỉ tài khoản có mật khẩu (HR doanh nghiệp) mới đặt lại ở đây; người dùng LMS đặt lại ở LMS.
        if (user == null || user.getPasswordHash() == null) {
            return;
        }
        long ttl = properties.getAuth().getResetTokenTtlMinutes();
        String rawToken = newToken();

        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(clock.instant().plus(Duration.ofMinutes(ttl)));
        tokenRepository.save(token);

        String link = properties.getWeb().getBaseUrl() + "/reset-password/" + rawToken;
        mailService.sendPasswordReset(user.getEmail(), user.getFullName(), link, ttl);
    }

    @Override
    @Transactional
    public void reset(ResetPasswordRequest request) {
        Instant now = clock.instant();
        PasswordResetToken token = tokenRepository.findByTokenHash(hash(request.getToken()))
                .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(now))
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_TOKEN_INVALID));
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESET_TOKEN_INVALID));

        token.setUsedAt(now);
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        user.setFailedLoginCount(0);
        user.setLockedUntil(null);
        user.setStatus(UserStatus.ACTIVE);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
