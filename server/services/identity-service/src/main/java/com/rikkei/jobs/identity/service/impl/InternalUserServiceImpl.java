package com.rikkei.jobs.identity.service.impl;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.identity.config.AppProperties;
import com.rikkei.jobs.identity.config.exception.ApiException;
import com.rikkei.jobs.identity.config.exception.ErrorCode;
import com.rikkei.jobs.identity.dto.request.InternalCreateUserRequest;
import com.rikkei.jobs.identity.dto.response.InternalUserResponse;
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

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalUserServiceImpl implements InternalUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final AppProperties properties;

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
}
