package com.rikkei.jobs.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkei.jobs.identity.config.DevDataSeeder;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Tài khoản HR và staff mẫu phải có id cố định để company-service seed company_hr khớp. */
@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:identity-seed;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "app.dev-data.enabled=true",
            "app.lms.base-url=http://localhost:0",
            "app.mail.enabled=false"
        })
class DevDataSeederIdsTest {

    @Autowired UserRepository users;

    @Test
    void sampleAccountsHaveFixedIds() {
        assertThat(users.findByEmail("hr1@rikkeijobs.test")).get().extracting(User::getId).isEqualTo(DevDataSeeder.HR1_ID);
        assertThat(users.findByEmail("hr2@rikkeijobs.test")).get().extracting(User::getId).isEqualTo(DevDataSeeder.HR2_ID);
        assertThat(users.findByEmail("hr-new@rikkeijobs.test")).get().extracting(User::getId).isEqualTo(DevDataSeeder.HR_NEW_ID);
        assertThat(users.findByEmail("staff1@rikkeijobs.test")).get().extracting(User::getId).isEqualTo(DevDataSeeder.STAFF1_ID);
    }
}
