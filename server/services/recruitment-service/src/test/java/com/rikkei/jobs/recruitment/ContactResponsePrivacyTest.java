package com.rikkei.jobs.recruitment;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
class ContactResponsePrivacyTest extends ContactIntegrationSupport {
    @Test void hrDetailAndListNeverExposeStructuredContacts() throws Exception {
        var id=apply();
        for(String url:new String[]{"/recruitment/applications/"+id,"/recruitment/hr/applications"}) {
            String response=mvc.perform(get(url).header("Authorization",hr())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(response).doesNotContain("candidate-canary@example.test","0901234567");
        }
        assertThat(count("contact_access_logs")).isZero();
    }
}
