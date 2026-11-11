package com.rikkei.jobs.company.config;

import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Hai doanh nghiệp mẫu theo docs/conventions.md (mục 4). ID khớp {@code companyId} của hr1 (Công ty A) và
 * hr2 (Công ty B) mà identity-service seed. Chạy lại không tạo trùng. Chỉ bật ở dev (app.dev-data.enabled=true).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.dev-data.enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    public static final UUID COMPANY_A_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    public static final UUID COMPANY_B_ID = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    private final CompanyRepository companyRepository;

    @Override
    public void run(ApplicationArguments args) {
        company(COMPANY_A_ID, "Công ty A");
        company(COMPANY_B_ID, "Công ty B");
    }

    private void company(UUID id, String name) {
        if (companyRepository.existsById(id)) {
            return;
        }
        companyRepository.save(new Company(id, name));
        log.info("Seeded company {} ({})", name, id);
    }
}
