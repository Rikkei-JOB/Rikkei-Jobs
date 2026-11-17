package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.company.client.IdentityClient;
import com.rikkei.jobs.company.client.IdentityUserStatus;
import com.rikkei.jobs.company.dto.response.StaffHrAccount;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
import com.rikkei.jobs.company.service.HrAccountLockService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Identity là nguồn sự thật; company_hr.locked_at chỉ là bản sao để chặn nhanh. Cố ý không
 * @Transactional: lời gọi HTTP không nằm trong transaction, và identity lỗi thì bản sao không đổi.
 */
@Service
@RequiredArgsConstructor
public class HrAccountLockServiceImpl implements HrAccountLockService {

    private final CompanyHrRepository companyHrRepository;
    private final IdentityClient identity;

    @Override
    public StaffHrAccount lock(UUID userId, String reason) {
        CompanyHr hr = findHr(userId);
        IdentityUserStatus result = identity.lockUser(userId, reason.strip());
        hr.setLockedAt(result.lockedAt() != null ? result.lockedAt() : Instant.now());
        companyHrRepository.save(hr);
        return StaffHrAccountMapping.from(result);
    }

    @Override
    public StaffHrAccount unlock(UUID userId) {
        CompanyHr hr = findHr(userId);
        IdentityUserStatus result = identity.unlockUser(userId);
        hr.setLockedAt(null);
        companyHrRepository.save(hr);
        return StaffHrAccountMapping.from(result);
    }

    private CompanyHr findHr(UUID userId) {
        return companyHrRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản HR"));
    }
}
