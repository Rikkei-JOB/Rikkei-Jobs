package com.rikkei.jobs.company.service.impl;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.client.EmailExistsException;
import com.rikkei.jobs.company.client.IdentityClient;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.dto.request.CreateHrAccountRequest;
import com.rikkei.jobs.company.dto.response.HrAccountResponse;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.service.HrAccountService;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Cố ý không @Transactional: lời gọi HTTP sang identity không được nằm trong transaction đang mở. */
@Slf4j
@Service
@RequiredArgsConstructor
public class HrAccountServiceImpl implements HrAccountService {

    private final CompanyRepository companyRepository;
    private final CompanyHrRepository companyHrRepository;
    private final IdentityClient identity;

    @Override
    public HrAccountResponse create(UUID companyId, CreateHrAccountRequest request) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công ty"));
        if (company.getStatus() == CompanyStatus.LOCKED) {
            throw new CompanyException(CompanyErrorCode.COMPANY_LOCKED);
        }
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        String fullName = request.getFullName().trim();

        UUID userId;
        boolean createdHere = true;
        try {
            userId = identity.createHrUser(email, fullName, companyId).userId();
        } catch (EmailExistsException e) {
            // Gọi lại sau timeout: identity đã tạo HR của công ty này nhưng liên kết chưa được ghi thì nối tiếp.
            boolean orphanOfThisCompany = e.role() == Role.HR && companyId.equals(e.companyId())
                    && e.userId() != null && !companyHrRepository.existsById(e.userId());
            if (!orphanOfThisCompany) {
                throw new ConflictException("Email đã được sử dụng");
            }
            userId = e.userId();
            createdHere = false;
        }

        try {
            companyHrRepository.saveAndFlush(new CompanyHr(userId, companyId));
        } catch (RuntimeException cause) {
            if (!createdHere) {
                // Nhánh nối lại: user không do request này tạo nên tuyệt đối không xoá. Có thể request song song
                // vừa nối xong thì coi là thành công, ngược lại là xung đột.
                boolean linkedHere = companyHrRepository.findById(userId)
                        .map(link -> companyId.equals(link.getCompanyId())).orElse(false);
                if (!linkedHere) {
                    throw new ConflictException("Email đã được sử dụng");
                }
            } else {
                rollbackIdentityUser(userId);
                throw new IllegalStateException("Không ghi được liên kết HR với công ty", cause);
            }
        }
        return HrAccountResponse.builder()
                .userId(userId).email(email).fullName(fullName).companyId(companyId).build();
    }

    private void rollbackIdentityUser(UUID userId) {
        try {
            identity.deleteUser(userId);
        } catch (RuntimeException e) {
            log.error("Không xoá được user HR {} sau khi ghi company_hr lỗi", userId, e);
        }
    }
}
