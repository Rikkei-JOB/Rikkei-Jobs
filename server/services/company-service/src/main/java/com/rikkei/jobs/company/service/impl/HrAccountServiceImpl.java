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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Lời gọi HTTP tạo user ở identity nằm ngoài transaction. Riêng bước ghi company_hr chạy trong một transaction
 * ngắn giữ khoá dòng công ty (cùng khoá với khoá / mở khoá công ty của DN-07), kiểm tra lại LOCKED trước khi ghi:
 * HR mới không lọt vào giữa lúc công ty đang bị khoá (spec DN-07 §4.5.6).
 */
@Slf4j
@Service
public class HrAccountServiceImpl implements HrAccountService {

    private final CompanyRepository companyRepository;
    private final CompanyHrRepository companyHrRepository;
    private final IdentityClient identity;
    private final TransactionTemplate tx;

    public HrAccountServiceImpl(CompanyRepository companyRepository, CompanyHrRepository companyHrRepository,
                                IdentityClient identity, PlatformTransactionManager transactionManager) {
        this.companyRepository = companyRepository;
        this.companyHrRepository = companyHrRepository;
        this.identity = identity;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    public HrAccountResponse create(UUID companyId, CreateHrAccountRequest request) {
        // Kiểm tra sớm (không khoá) để công ty đã khoá bị từ chối mà không gọi identity.
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
            linkUnderCompanyLock(userId, companyId);
        } catch (CompanyException | ResourceNotFoundException blocked) {
            // Công ty vừa bị khoá (hoặc không còn) trong lúc gọi identity: bỏ user vừa tạo. Nhánh nối lại thì user
            // không do request này tạo nên không xoá.
            if (createdHere) {
                rollbackIdentityUser(userId);
            }
            throw blocked;
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

    /** SELECT … FOR UPDATE công ty, kiểm tra lại LOCKED, rồi ghi liên kết trong cùng transaction. */
    private void linkUnderCompanyLock(UUID userId, UUID companyId) {
        tx.executeWithoutResult(s -> {
            Company locked = companyRepository.findByIdForUpdate(companyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công ty"));
            if (locked.getStatus() == CompanyStatus.LOCKED) {
                throw new CompanyException(CompanyErrorCode.COMPANY_LOCKED);
            }
            companyHrRepository.saveAndFlush(new CompanyHr(userId, companyId));
        });
    }

    private void rollbackIdentityUser(UUID userId) {
        try {
            identity.deleteUser(userId);
        } catch (RuntimeException e) {
            log.error("Không xoá được user HR {} sau khi ghi company_hr lỗi", userId, e);
        }
    }
}
