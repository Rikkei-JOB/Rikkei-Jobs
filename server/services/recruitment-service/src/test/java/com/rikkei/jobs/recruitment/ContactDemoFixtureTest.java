package com.rikkei.jobs.recruitment;
import java.nio.file.*;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.*;

/** Verify the delivered SQL against actual sibling-service migrations, only inside the test DB. */
class ContactDemoFixtureTest extends ApplicationIntegrationSupport {
    @Autowired DataSource datasource;
    @Test void seedIsRunnableAndPreservesReviewDecisionsOnRerun() throws Exception {
        for(String schema:new String[]{"identity","profile","company"}) {
            Flyway.configure().dataSource(datasource).schemas(schema).defaultSchema(schema)
                .locations("filesystem:"+Path.of("../"+schema+"-service/src/main/resources/db/migration").toAbsolutePath())
                .load().migrate();
        }
        UUID staff=UUID.fromString("00000000-0000-0000-0001-000000000005");
        UUID companyA=UUID.fromString("00000000-0000-0000-0000-0000000000a1");
        for(int i=1;i<=3;i++)jdbc.update("insert into identity.users(id,email,full_name,role) values (?,?,?,'CANDIDATE') on conflict(email) do nothing",
            UUID.randomUUID(),"candidate"+i+"@rikkeijobs.test","Candidate "+i);
        jdbc.update("insert into company.companies(id,name,tax_code,created_by) values (?,'Company A','0300000001',?) on conflict(id) do nothing",companyA,staff);
        jdbc.update("insert into company.jobs(id,company_id,title,status,created_by) values (?,?,'Java','APPROVED',?) on conflict(id) do nothing",
            UUID.fromString("03000000-0000-4000-8000-000000000077"),companyA,staff);
        String sql=Files.readString(Path.of("../../../docs/api/fixtures/ut03-contact-demo.sql"));
        jdbc.execute(sql);
        assertThat(count("contact_requests")).isEqualTo(3);
        assertThat(count("applications")).isEqualTo(1);
        jdbc.update("update recruitment.contact_requests set status='REJECTED',reviewed_by=?,reviewed_at=now(),rejection_reason='Keep decision' where status='PENDING'",staff);
        jdbc.execute(sql);
        assertThat(count("contact_requests")).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from recruitment.contact_requests where rejection_reason='Keep decision'",Integer.class)).isEqualTo(1);
    }
}
