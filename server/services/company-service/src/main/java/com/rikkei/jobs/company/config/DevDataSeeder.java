package com.rikkei.jobs.company.config;

import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
import com.rikkei.jobs.company.repository.CompanyRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Dữ liệu mẫu: Công ty A, Công ty B và liên kết HR theo tài khoản mẫu của identity-service.
 * Chạy lại không tạo bản ghi trùng. Chỉ bật ở dev/test (app.dev-data.enabled=true).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dev-data.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    // Các UUID dưới đây phải khớp với DevDataSeeder của identity-service (company-service không phụ thuộc identity).
    static final UUID COMPANY_A_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    static final UUID COMPANY_B_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    static final UUID HR1_ID = UUID.fromString("00000000-0000-0000-0001-000000000001");
    static final UUID HR2_ID = UUID.fromString("00000000-0000-0000-0001-000000000002");
    static final UUID HR_NEW_ID = UUID.fromString("00000000-0000-0000-0001-000000000003");
    static final UUID STAFF1_ID = UUID.fromString("00000000-0000-0000-0001-000000000005");

    private final CompanyRepository companyRepository;
    private final CompanyHrRepository companyHrRepository;

    @Override
    public void run(ApplicationArguments args) {
        company(COMPANY_A_ID, "Công ty A", "0101234567", "Phần mềm", "51-200", "Hà Nội");
        company(COMPANY_B_ID, "Công ty B", "0109876543210", "Thương mại điện tử", "201-1000", "Hồ Chí Minh");
        link(HR1_ID, COMPANY_A_ID);
        link(HR_NEW_ID, COMPANY_A_ID);
        link(HR2_ID, COMPANY_B_ID);
    }

    private void company(UUID id, String name, String taxCode, String industry, String size, String city) {
        if (companyRepository.existsById(id)) {
            return;
        }
        Company company = new Company();
        company.setId(id);
        company.setName(name);
        company.setTaxCode(taxCode);
        company.setIndustry(industry);
        company.setSize(size);
        company.setCity(city);
        company.setStatus(CompanyStatus.ACTIVE);
        company.setCreatedBy(STAFF1_ID);
        companyRepository.save(company);
        log.info("Đã tạo công ty mẫu {}", name);
    }

    private void link(UUID userId, UUID companyId) {
        if (companyHrRepository.existsById(userId)) {
            return;
        }
        companyHrRepository.save(new CompanyHr(userId, companyId));
    }
}
