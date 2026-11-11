package com.rikkei.jobs.identity.service.impl;

import com.rikkei.jobs.identity.config.exception.ApiException;
import com.rikkei.jobs.identity.config.exception.ErrorCode;
import com.rikkei.jobs.identity.dto.response.LmsTicketInfo;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.repository.UserRepository;
import com.rikkei.jobs.identity.service.SsoUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SsoUserServiceImpl implements SsoUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public User findOrCreate(LmsTicketInfo info) {
        String email = info.getEmail().trim().toLowerCase();

        Optional<User> byLms = userRepository.findByLmsUserId(info.getLmsUserId());
        if (byLms.isPresent()) {
            return byLms.get();
        }

        Optional<User> byEmail = userRepository.findByEmail(email);
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            // Không cho chiếm tài khoản doanh nghiệp hoặc tài khoản đã gắn với danh tính LMS khác.
            if (!existing.getRole().isLmsRole() || existing.getLmsUserId() != null) {
                throw new ApiException(ErrorCode.INVALID_TICKET);
            }
            existing.setLmsUserId(info.getLmsUserId());
            return existing;
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(info.getFullName() == null || info.getFullName().isBlank() ? email : info.getFullName().trim());
        user.setRole(info.getRole());
        user.setLmsUserId(info.getLmsUserId());
        return userRepository.saveAndFlush(user);
    }
}
