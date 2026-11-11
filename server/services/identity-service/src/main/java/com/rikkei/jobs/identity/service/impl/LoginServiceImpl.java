package com.rikkei.jobs.identity.service.impl;

import com.rikkei.jobs.identity.config.AppProperties;
import com.rikkei.jobs.identity.config.exception.ApiException;
import com.rikkei.jobs.identity.config.exception.ErrorCode;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.entity.UserStatus;
import com.rikkei.jobs.identity.repository.UserRepository;
import com.rikkei.jobs.identity.service.LoginService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties properties;
    private final Clock clock;

    // So sánh với hash này khi email không tồn tại, để thời gian phản hồi không lộ email có tồn tại hay không.
    private String dummyHash;

    @PostConstruct
    void init() {
        dummyHash = passwordEncoder.encode("not-a-real-password-1");
    }

    /**
     * Bộ đếm sai mật khẩu phải được lưu dù trả về lỗi 401, nên exception không làm rollback transaction.
     */
    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public User authenticate(String email, String password) {
        Instant now = clock.instant();
        User user = userRepository.findByEmail(email.trim().toLowerCase()).orElse(null);

        if (user == null || user.getPasswordHash() == null) {
            passwordEncoder.matches(password, dummyHash);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Đang bị khoá: từ chối cả khi nhập đúng mật khẩu.
        if (user.isLockedAt(now)) {
            throw locked(user, now);
        }
        // Hết thời gian khoá: đếm lại từ 0.
        if (user.getStatus() == UserStatus.LOCKED || user.getLockedUntil() != null) {
            user.setStatus(UserStatus.ACTIVE);
            user.setLockedUntil(null);
            user.setFailedLoginCount(0);
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            int failed = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount(failed);
            if (failed >= properties.getAuth().getMaxFailedAttempts()) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(now.plus(Duration.ofMinutes(properties.getAuth().getLockMinutes())));
            }
            userRepository.save(user);
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.setFailedLoginCount(0);
        return userRepository.save(user);
    }

    private ApiException locked(User user, Instant now) {
        long seconds = Duration.between(now, user.getLockedUntil()).getSeconds();
        long minutes = Math.max(1, (seconds + 59) / 60);
        return new ApiException(
                ErrorCode.ACCOUNT_LOCKED,
                "Tài khoản đang bị khoá. Vui lòng thử lại sau " + minutes + " phút.",
                Map.of("minutesRemaining", minutes, "lockedUntil", user.getLockedUntil().toString()));
    }
}
