package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.common.config.security.principle.Role;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class ContactAccessLogApiTest extends ContactIntegrationSupport {
    void log(UUID candidate,String time) {
        jdbc.update("insert into recruitment.contact_access_logs values (?,?,?,?,'APPLICATION',?::timestamptz)",UUID.randomUUID(),company,hr,candidate,time);
    }
    @Test void candidateCanOnlyReadOwnLogsAndStaffHasHalfOpenRange() throws Exception {
        log(user,"2030-01-01T00:00:00Z");log(user,"2030-01-02T00:00:00Z");log(UUID.randomUUID(),"2030-01-01T00:00:00Z");
        mvc.perform(get("/me/contact-access-logs").param("userId",UUID.randomUUID().toString()).header("Authorization",candidate()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.items[0].userId").value(user.toString()))
            .andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get("/staff/contact-access-logs").param("companyId",company.toString()).param("userId",user.toString())
            .param("from","2030-01-01T00:00:00Z").param("to","2030-01-02T00:00:00Z").header("Authorization",staff()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].companyName").value("Công ty A"));
    }
    @Test void invalidRangesPagingAndRolesAreRejected() throws Exception {
        mvc.perform(get("/staff/contact-access-logs").param("from","2030-01-02T00:00:00Z").param("to","2030-01-01T00:00:00Z").header("Authorization",staff())).andExpect(status().isBadRequest());
        for(String value:List.of("2030-01-01","2030-01-01T00:00:00"))mvc.perform(get("/staff/contact-access-logs").param("from",value).header("Authorization",staff())).andExpect(status().isBadRequest());
        mvc.perform(get("/me/contact-access-logs").param("size","101").header("Authorization",candidate())).andExpect(status().isBadRequest());
        for(Role role:List.of(Role.HR,Role.ADMIN,Role.CANDIDATE))mvc.perform(get("/staff/contact-access-logs").header("Authorization",token(hr,role,company))).andExpect(status().isForbidden());
    }
}
