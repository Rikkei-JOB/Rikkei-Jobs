package com.rikkei.jobs.company.support;

import com.rikkei.jobs.common.config.security.jwt.JwtUtils;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyHr;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.repository.CompanyHrRepository;
import com.rikkei.jobs.company.repository.CompanyRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Nền chung cho test API của company-service: H2 (MODE=PostgreSQL) với schema company, JWT thật. */
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:company;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "app.jwt.secret=test_secret_test_secret_test_secret_test_secret",
    "app.dev-data.enabled=false"
})
@AutoConfigureMockMvc
public abstract class CompanyApiTestSupport {

    protected static final UUID HR1 = UUID.fromString("00000000-0000-0000-0001-000000000001");
    protected static final UUID HR2 = UUID.fromString("00000000-0000-0000-0001-000000000002");

    private static final Path UPLOAD_DIR = createUploadDir();

    @Autowired protected MockMvc mvc;
    @Autowired protected JwtUtils jwt;
    @Autowired protected CompanyRepository companies;
    @Autowired protected CompanyHrRepository companyHrs;

    @DynamicPropertySource
    static void uploadDir(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", UPLOAD_DIR::toString);
    }

    private static Path createUploadDir() {
        try {
            return Files.createTempDirectory("company-uploads");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeEach
    void cleanTables() {
        companyHrs.deleteAll();
        companies.deleteAll();
    }

    protected String token(Role role) {
        return token(UUID.randomUUID(), role);
    }

    protected String token(UUID userId, Role role) {
        return jwt.generateJwtToken(userId, role, null);
    }

    protected Company saveCompany(String taxCode, CompanyStatus status) {
        Company company = new Company();
        company.setName("Công ty " + taxCode);
        company.setTaxCode(taxCode);
        company.setStatus(status);
        company.setCreatedBy(UUID.randomUUID());
        return companies.save(company);
    }

    protected void linkHr(UUID userId, UUID companyId) {
        companyHrs.save(new CompanyHr(userId, companyId));
    }
}
