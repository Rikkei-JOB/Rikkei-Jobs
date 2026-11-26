package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.service.ContactAccessService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.*;

class ContactPrivacyPersistenceTest extends ApplicationIntegrationSupport {
    @Autowired ContactAccessService access;
    @Test void withdrawnAndRejectedStillGrantAccess() throws Exception {
        UUID application = apply();
        for (String status : new String[]{"WITHDRAWN","REJECTED"}) {
            jdbc.update("update recruitment.applications set status=? where id=?",status,application);
            assertThat(access.canSeeContact(company,user)).isTrue();
            assertThat(access.canSeeContact(UUID.randomUUID(),user)).isFalse();
        }
    }
    @Test void pendingAndRejectedRequestsDoNotGrantAndPendingIsUnique() {
        UUID id = UUID.randomUUID();
        insert(id,"PENDING");
        assertThat(access.canSeeContact(company,user)).isFalse();
        assertThatThrownBy(()->insert(UUID.randomUUID(),"PENDING")).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        jdbc.update("update recruitment.contact_requests set status='REJECTED',reviewed_by=?,reviewed_at=now(),rejection_reason='Không phù hợp' where id=?",UUID.randomUUID(),id);
        assertThat(access.canSeeContact(company,user)).isFalse();
        insert(UUID.randomUUID(),"PENDING");
        assertThatThrownBy(()->jdbc.update("insert into recruitment.contact_access_logs values (?,?,?,?,'INVALID',now())",
            UUID.randomUUID(),company,UUID.randomUUID(),user)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    private void insert(UUID id,String status) {
        jdbc.update("insert into recruitment.contact_requests(id,company_id,user_id,job_id,reason,status,created_by,created_at) values (?,?,?,?,?,?,?,now())",
            id,company,user,job,"Lý do",status,UUID.randomUUID());
    }
}
