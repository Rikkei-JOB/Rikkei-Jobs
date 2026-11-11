package com.rikkei.jobs.company.service.impl;

import static com.rikkei.jobs.company.mapper.CompanyMapper.blankToNull;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyHrRepository companyHrRepository;
    private final CompanyMapper companyMapper;

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
}
