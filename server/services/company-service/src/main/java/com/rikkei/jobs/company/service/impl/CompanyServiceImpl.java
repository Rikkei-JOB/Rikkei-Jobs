package com.rikkei.jobs.company.service.impl;

import static com.rikkei.jobs.company.mapper.CompanyMapper.blankToNull;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.access.OwnershipGuard;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.config.exception.CompanyErrorCode;
import com.rikkei.jobs.company.config.exception.CompanyException;
import com.rikkei.jobs.company.dto.request.CreateCompanyRequest;
import com.rikkei.jobs.company.dto.request.UpdateCompanyRequest;
import com.rikkei.jobs.company.dto.response.CompanyResponse;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.mapper.CompanyMapper;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
import com.rikkei.jobs.company.repository.CompanyRepository;
import com.rikkei.jobs.company.service.CompanyService;
import com.rikkei.jobs.company.service.LogoStorage;
import com.rikkei.jobs.company.util.CompanyRules;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyHrRepository companyHrRepository;
    private final CompanyMapper companyMapper;
    private final LogoStorage logoStorage;

    private static final String LOGO_URL_PREFIX = "/companies/logos/";

    @Override
    @Transactional
    public CompanyResponse create(CreateCompanyRequest request, UUID staffUserId) {
        String taxCode = request.getTaxCode().strip();
        // Kiểm tra trước để trả 409 rõ ràng; ràng buộc UNIQUE của DB là lưới an toàn khi hai request đua nhau.
        if (companyRepository.existsByTaxCode(taxCode)) {
            throw new ConflictException("Mã số thuế đã tồn tại");
        }
        Company company = new Company();
        company.setName(request.getName().strip());
        company.setTaxCode(taxCode);
        company.setIndustry(blankToNull(request.getIndustry()));
        company.setSize(blankToNull(request.getSize()));
        company.setAddress(blankToNull(request.getAddress()));
        company.setCity(blankToNull(request.getCity()));
        company.setWebsite(blankToNull(request.getWebsite()));
        company.setDescription(blankToNull(request.getDescription()));
        company.setStatus(CompanyStatus.ACTIVE);
        company.setCreatedBy(staffUserId);
        return companyMapper.toResponse(companyRepository.save(company));
    }

    @Override
    @Transactional(readOnly = true)
    public Company findMine(UUID userId) {
        CompanyHr link = companyHrRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công ty"));
        return companyRepository.findById(link.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy công ty"));
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getMine(UUID userId) {
        return companyMapper.toResponse(findMine(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyResponse getCompany(AuthPrincipal principal, UUID companyId) {
        String notFound = "Không tìm thấy công ty";
        if (!OwnershipGuard.seesAllData(principal)) {
            // HR: công ty khác và công ty không tồn tại trả cùng một lỗi 404, không lộ id có thật.
            boolean ownsIt = principal.getRole() == Role.HR
                    && companyHrRepository.findById(principal.getUserId())
                            .map(link -> link.getCompanyId().equals(companyId))
                            .orElse(false);
            if (!ownsIt) {
                throw OwnershipGuard.notFound(notFound);
            }
        }
        return companyRepository.findById(companyId)
                .map(companyMapper::toResponse)
                .orElseThrow(() -> OwnershipGuard.notFound(notFound));
    }

    @Override
    @Transactional
    public CompanyResponse updateMine(UUID userId, UpdateCompanyRequest request) {
        Company company = findMine(userId);
        if (company.getStatus() == CompanyStatus.LOCKED) {
            throw new CompanyException(CompanyErrorCode.COMPANY_LOCKED);
        }
        company.setName(request.getName().strip());
        company.setIndustry(blankToNull(request.getIndustry()));
        company.setSize(blankToNull(request.getSize()));
        company.setAddress(blankToNull(request.getAddress()));
        company.setCity(blankToNull(request.getCity()));
        company.setWebsite(blankToNull(request.getWebsite()));
        company.setDescription(blankToNull(request.getDescription()));
        return companyMapper.toResponse(companyRepository.save(company));
    }

    /**
     * Cố ý không @Transactional bao ngoài: ghi tệp mới trước, lưu DB (giao dịch riêng của save),
     * chỉ xoá tệp cũ sau khi DB đã cập nhật để lỗi không bao giờ để công ty trỏ vào tệp không còn.
     */
    @Override
    public CompanyResponse updateMyLogo(UUID userId, MultipartFile file) {
        Company company = findMine(userId);
        if (company.getStatus() == CompanyStatus.LOCKED) {
            throw new CompanyException(CompanyErrorCode.COMPANY_LOCKED);
        }
        if (file.getSize() > CompanyRules.LOGO_MAX_BYTES) {
            throw new CompanyException(CompanyErrorCode.FILE_TOO_LARGE);
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String extension = detectExtension(content);
        if (extension == null) {
            throw new CompanyException(CompanyErrorCode.INVALID_FILE_TYPE);
        }

        String oldFileName = company.getLogoUrl() != null && company.getLogoUrl().startsWith(LOGO_URL_PREFIX)
                ? company.getLogoUrl().substring(LOGO_URL_PREFIX.length()) : null;
        String newFileName = logoStorage.store(content, extension);
        Company saved;
        try {
            company.setLogoUrl(LOGO_URL_PREFIX + newFileName);
            saved = companyRepository.save(company);
        } catch (RuntimeException e) {
            logoStorage.delete(newFileName);
            throw e;
        }
        if (oldFileName != null) {
            logoStorage.delete(oldFileName);
        }
        return companyMapper.toResponse(saved);
    }

    /** Nhận dạng theo chữ ký tệp: JPG (FF D8 FF) hoặc PNG (89 50 4E 47 0D 0A 1A 0A); khác (kể cả rỗng) thì null. */
    private static String detectExtension(byte[] c) {
        if (c.length >= 3 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8 && (c[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        int[] png = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (c.length < png.length) {
            return null;
        }
        for (int i = 0; i < png.length; i++) {
            if ((c[i] & 0xFF) != png[i]) {
                return null;
            }
        }
        return "png";
    }
}
