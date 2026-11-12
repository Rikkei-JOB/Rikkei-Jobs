package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.client.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {"app.recruitment.outbox-enabled=false", "app.jwt.secret=test_secret_test_secret_test_secret_test_secret"})
class RecruitmentConfigurationSmokeTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) { ApplicationIntegrationSupport.database(properties); }
    @Autowired JobApplicationClient jobs;
    @Autowired ProfileCvClient cvs;

    @Test void productionClientsStartWithPrivatePathsAndResolveNestedUriPlaceholders() {
        assertThat(jobs).isInstanceOf(HttpJobApplicationClient.class);
        assertThat(cvs).isInstanceOf(HttpProfileCvClient.class);
        assertThat(ReflectionTestUtils.getField(jobs, "path")).isEqualTo("/internal/jobs/{jobId}/application-context");
        assertThat(ReflectionTestUtils.getField(cvs, "path")).isEqualTo("/profile/cvs/{cvId}/pdf");
    }
}
