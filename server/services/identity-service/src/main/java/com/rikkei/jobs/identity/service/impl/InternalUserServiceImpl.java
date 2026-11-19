package com.rikkei.jobs.identity.service.impl;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.config.AppProperties;
import com.rikkei.jobs.identity.config.exception.ApiException;
import com.rikkei.jobs.identity.config.exception.ErrorCode;
import com.rikkei.jobs.identity.dto.request.InternalCreateUserRequest;
import com.rikkei.jobs.identity.dto.response.InternalUserResponse;
import com.rikkei.jobs.identity.dto.response.InternalUserStatusResponse;
import com.rikkei.jobs.identity.dto.response.InternalLmsIdentityResponse;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.entity.UserStatus;
import com.rikkei.jobs.identity.repository.UserRepository;
import com.rikkei.jobs.identity.service.InternalUserService;
import com.rikkei.jobs.identity.service.MailService;
import com.rikkei.jobs.identity.util.TempPasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalUserServiceImpl implements InternalUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AppProperties properties;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public InternalUserResponse getCandidate(UUID id) {
        User user = userRepository.findById(id)
                .filter(u -> u.getRole() == Role.CANDIDATE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ứng viên"));
        return new InternalUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getCompanyId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalUserResponse> getCandidates(List<UUID> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > 100 || ids.stream().anyMatch(java.util.Objects::isNull))
            throw new com.rikkei.jobs.common.config.exception.BadRequestException("Danh sách ứng viên phải có 1-100 ID.");
        var found = userRepository.findAllById(ids).stream()
            .filter(u -> u.getRole() == Role.CANDIDATE)
            .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
        return ids.stream().distinct().map(id -> {
            var user = found.get(id);
            if (user == null) throw new ResourceNotFoundException("Không tìm thấy ứng viên");
            return new InternalUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getCompanyId());
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InternalLmsIdentityResponse getLmsIdentity(UUID id) {
        User user = userRepository.findById(id)
                .filter(u -> u.getRole() == Role.CANDIDATE && u.getStatus() == UserStatus.ACTIVE)
                .filter(u -> u.getLmsUserId() != null && !u.getLmsUserId().isBlank())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy định danh LMS"));
        return new InternalLmsIdentityResponse(user.getId(), user.getLmsUserId());
    }

    @Override
    @Transactional
    public InternalUserResponse createUser(InternalCreateUserRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            User u = existing.get();
            // Map.of cấm giá trị null nên chỉ đưa vào các key có giá trị.
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("userId", u.getId());
            if (u.getRole() != null) {
                details.put("role", u.getRole());
            }
            if (u.getCompanyId() != null) {
                details.put("companyId", u.getCompanyId());
            }
            throw new ApiException(ErrorCode.EMAIL_EXISTS, ErrorCode.EMAIL_EXISTS.defaultMessage(), details);
        }

        String tempPassword = TempPasswordGenerator.generate();
        User user = new User();
        user.setEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setRole(request.getRole());
        user.setCompanyId(request.getCompanyId());
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setMustChangePassword(true);
        user = userRepository.saveAndFlush(user);

        String loginUrl = properties.getWeb().getBaseUrl() + "/login";
        try {
            mailService.sendHrAccountCreated(email, user.getFullName(), tempPassword, loginUrl);
        } catch (MailException e) {
            // ApiException là RuntimeException nên giao dịch rollback: không để lại user mà HR không nhận được mật khẩu.
            log.warn("Không gửi được email tài khoản HR tới {}: {}", email, e.getMessage());
            throw new ApiException(ErrorCode.MAIL_UNAVAILABLE);
        }
        return new InternalUserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getCompanyId());
    }

    @Override
    @Transactional
    public void deleteHrUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        if (user.getRole() != Role.HR) {
            throw new ConflictException("Chỉ xoá được tài khoản HR");
        }
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public InternalUserStatusResponse lockHr(UUID id, String reason) {
        User user = findHr(id, "Chỉ khoá được tài khoản HR");
        Instant now = clock.instant();
        user.setStatus(UserStatus.LOCKED);
        user.setLockedReason(reason);
        user.setLockedAt(now);
        return InternalUserStatusResponse.from(userRepository.save(user), now);
    }

    @Override
    @Transactional
    public InternalUserStatusResponse unlockHr(UUID id) {
        User user = findHr(id, "Chỉ mở khoá được tài khoản HR");
        // Không bị cán bộ khoá thì không đụng tới khoá tạm của NT-01 (tự hết hạn).
        if (user.isAdminLocked()) {
            user.setStatus(UserStatus.ACTIVE);
            user.setLockedReason(null);
            user.setLockedAt(null);
            user.setLockedUntil(null);
            user.setFailedLoginCount(0);
            user = userRepository.save(user);
        }
        return InternalUserStatusResponse.from(user, clock.instant());
    }

    @Override
    @Transactional
    public List<InternalUserStatusResponse> setCompanyLock(List<UUID> ids, boolean locked) {
        Instant now = clock.instant();
        Set<UUID> distinct = new LinkedHashSet<>(ids);
        Map<UUID, User> byId = new HashMap<>();
        userRepository.findAllById(distinct).forEach(u -> byId.put(u.getId(), u));
        return distinct.stream()
                .map(byId::get)
                .filter(u -> u != null && u.getRole() == Role.HR)
                .map(u -> {
                    if (locked) {
                        if (u.getCompanyLockedAt() == null) {
                            u.setCompanyLockedAt(now);
                        }
                    } else {
                        u.setCompanyLockedAt(null);
                    }
                    return InternalUserStatusResponse.from(userRepository.save(u), now);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InternalUserStatusResponse> lookup(List<UUID> ids) {
        Instant now = clock.instant();
        Set<UUID> distinct = new LinkedHashSet<>(ids);
        // findAllById không bảo đảm thứ tự: dựng map rồi duyệt theo thứ tự id đầu vào đã bỏ trùng.
        Map<UUID, User> byId = new HashMap<>();
        userRepository.findAllById(distinct).forEach(u -> byId.put(u.getId(), u));
        return distinct.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(u -> InternalUserStatusResponse.from(u, now))
                .toList();
    }

    private User findHr(UUID id, String notHrMessage) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        if (user.getRole() != Role.HR) {
            throw new ConflictException(notHrMessage);
        }
        return user;
    }
}
