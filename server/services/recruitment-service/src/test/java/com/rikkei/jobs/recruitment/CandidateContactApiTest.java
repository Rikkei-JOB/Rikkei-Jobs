package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.client.HrAccountStatusClient;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class CandidateContactApiTest extends ContactIntegrationSupport {
    String root(){return "/candidates/"+user+"/contact";}
    @Test void lockedGets403AndCrossCompanyCannotRead() throws Exception {
        mvc.perform(get(root()).header("Authorization",hr())).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CONTACT_LOCKED"));
        apply();
        mvc.perform(get(root()).header("Authorization",token(UUID.randomUUID(),Role.HR,UUID.randomUUID()))).andExpect(status().isForbidden());
        assertThat(count("contact_access_logs")).isZero();
    }
    @Test void everyReadLogsCurrentHrAndApplicationTakesPrecedence() throws Exception {
        UUID application=apply();
        jdbc.update("update recruitment.applications set status='WITHDRAWN' where id=?",application);
        for(int i=0;i<2;i++) mvc.perform(get(root()).header("Authorization",hr())).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.email").value("candidate-canary@example.test")).andExpect(jsonPath("$.phone").value("0901234567"));
        assertThat(count("contact_access_logs")).isEqualTo(2);
        assertThat(jdbc.queryForList("select hr_id from recruitment.contact_access_logs",UUID.class)).containsOnly(hr);
        assertThat(jdbc.queryForList("select via from recruitment.contact_access_logs",String.class)).containsOnly("APPLICATION");
    }
    @Test void approvedRequestGrantsCompanyWideAccessAndMissingPhoneIsAllowed() throws Exception {
        UUID id=UUID.randomUUID();
        jdbc.update("insert into recruitment.contact_requests(id,company_id,user_id,job_id,reason,status,created_by,created_at,reviewed_by,reviewed_at) values (?,?,?,?,?,'APPROVED',?,now(),?,now())",id,company,user,job,"Hợp lệ",hr,staff);
        when(contacts.phone(eq(user),anyString())).thenReturn(null);
        UUID anotherHr=UUID.randomUUID();
        mvc.perform(get(root()).header("Authorization",token(anotherHr,Role.HR,company))).andExpect(status().isOk()).andExpect(jsonPath("$.phone").isEmpty());
        assertThat(jdbc.queryForObject("select via from recruitment.contact_access_logs",String.class)).isEqualTo("APPROVED_REQUEST");
        apply();
        mvc.perform(get(root()).header("Authorization",hr())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from recruitment.contact_access_logs where via='APPLICATION'",Integer.class)).isEqualTo(1);
    }
    @Test void dependencyFailureAndLockedHrNeverReturnContact() throws Exception {
        apply();
        when(contacts.phone(eq(user),anyString())).thenThrow(new RecruitmentDependencyException(503,"PROFILE_UNAVAILABLE","Unavailable"));
        mvc.perform(get(root()).header("Authorization",hr())).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.email").doesNotExist());
        assertThat(count("contact_access_logs")).isZero();
        when(hrAccounts.state(hr,company)).thenReturn(HrAccountStatusClient.State.LOCKED);
        mvc.perform(get(root()).header("Authorization",hr())).andExpect(status().isUnauthorized());
    }
}
