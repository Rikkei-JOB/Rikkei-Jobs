package com.rikkei.jobs.identity;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkei.jobs.identity.util.PasswordPolicy;
import com.rikkei.jobs.identity.util.TempPasswordGenerator;
import org.junit.jupiter.api.RepeatedTest;

class TempPasswordGeneratorTest {

    @RepeatedTest(200)
    void generatesPolicyCompliantPassword() {
        String p = TempPasswordGenerator.generate();
        assertThat(p).hasSize(12).matches(PasswordPolicy.REGEX);
    }
}
