package com.rikkei.jobs.recruitment;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.assertj.core.api.Assertions.*;
class ContactCommitFailureTest extends ContactIntegrationSupport {
    @Test void deferredCommitFailureNeverReturnsContactOrPersistsLog() throws Exception {
        apply();
        jdbc.execute("create or replace function recruitment.ut03_fail_commit() returns trigger language plpgsql as $$ begin raise exception 'test deferred commit failure'; end; $$");
        jdbc.execute("create constraint trigger ut03_commit_failure after insert on recruitment.contact_access_logs deferrable initially deferred for each row execute function recruitment.ut03_fail_commit()");
        try {
            var response=mvc.perform(get("/candidates/"+user+"/contact").header("Authorization",hr())).andReturn().getResponse();
            assertThat(response.getStatus()).isGreaterThanOrEqualTo(500);
            assertThat(response.getContentAsString()).doesNotContain("candidate-canary@example.test","0901234567");
            assertThat(count("contact_access_logs")).isZero();
        } finally {
            jdbc.execute("drop trigger ut03_commit_failure on recruitment.contact_access_logs");
            jdbc.execute("drop function recruitment.ut03_fail_commit()");
        }
    }
}
