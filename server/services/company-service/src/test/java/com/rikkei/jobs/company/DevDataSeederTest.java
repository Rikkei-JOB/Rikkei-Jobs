package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkei.jobs.company.config.DevDataSeeder;
import com.rikkei.jobs.company.entity.Company;
import com.rikkei.jobs.company.entity.CompanyStatus;
import com.rikkei.jobs.company.support.CompanyApiTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.dev-data.enabled=true")
class DevDataSeederTest extends CompanyApiTestSupport {

    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private static final UUID HR_NEW = UUID.fromString("00000000-0000-0000-0001-000000000003");
    private static final UUID STAFF1 = UUID.fromString("00000000-0000-0000-0001-000000000005");

    @Autowired DevDataSeeder seeder;

    @Test
    void seedsSampleCompaniesAndHrLinksAndIsIdempotent() throws Exception {
        // cleanTables() của lớp nền đã xoá dữ liệu do runner chạy lúc khởi động; seed lại từ đầu.
        seeder.run(null);
        assertSeeded();

        seeder.run(null);
        assertSeeded();
    }

    private void assertSeeded() {
        assertThat(companies.count()).isEqualTo(2);
        assertThat(companyHrs.count()).isEqualTo(3);

        Company a = companies.findById(A).orElseThrow();
        assertThat(a.getName()).isEqualTo("Công ty A");
        assertThat(a.getTaxCode()).isEqualTo("0101234567");
        assertThat(a.getIndustry()).isEqualTo("Phần mềm");
        assertThat(a.getSize()).isEqualTo("51-200");
        assertThat(a.getCity()).isEqualTo("Hà Nội");
        assertThat(a.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(a.getCreatedBy()).isEqualTo(STAFF1);

        Company b = companies.findById(B).orElseThrow();
        assertThat(b.getName()).isEqualTo("Công ty B");
        assertThat(b.getTaxCode()).isEqualTo("0109876543210");
        assertThat(b.getIndustry()).isEqualTo("Thương mại điện tử");
        assertThat(b.getSize()).isEqualTo("201-1000");
        assertThat(b.getCity()).isEqualTo("Hồ Chí Minh");
        assertThat(b.getStatus()).isEqualTo(CompanyStatus.ACTIVE);
        assertThat(b.getCreatedBy()).isEqualTo(STAFF1);

        assertThat(companyHrs.findById(HR1).orElseThrow().getCompanyId()).isEqualTo(A);
        assertThat(companyHrs.findById(HR_NEW).orElseThrow().getCompanyId()).isEqualTo(A);
        assertThat(companyHrs.findById(HR2).orElseThrow().getCompanyId()).isEqualTo(B);
    }
}
