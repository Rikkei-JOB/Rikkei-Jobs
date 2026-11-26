package com.rikkei.jobs.recruitment;
import com.rikkei.jobs.recruitment.client.*;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.junit.jupiter.api.BeforeEach;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
abstract class ContactIntegrationSupport extends ApplicationIntegrationSupport {
    @MockBean ContactProfileClient profiles;
    @MockBean ContactDirectoryClient directory;
    @MockBean IdentityCandidateClient identities;
    UUID hr=UUID.randomUUID(),staff=UUID.randomUUID();
    String hr(){return token(hr,com.rikkei.jobs.common.config.security.principle.Role.HR,company);}
    String staff(){return token(staff,com.rikkei.jobs.common.config.security.principle.Role.STAFF,null);}
    @BeforeEach void contactFixtures() {
        when(profiles.get(eq(user),anyString())).thenReturn(new ContactProfile(user,"Ứng viên A","PUBLIC"));
        when(directory.lookup(anySet(),anySet())).thenReturn(new ContactDirectory(Map.of(company,"Công ty A"),Map.of(hr,"HR A")));
        when(identities.get(user)).thenReturn(new CandidateIdentity(user,"Ứng viên A","candidate-canary@example.test"));
        when(identities.getBatch(anyList())).thenReturn(Map.of(user,new CandidateIdentity(user,"Ứng viên A","candidate-canary@example.test")));
        when(contacts.phone(eq(user),anyString())).thenReturn("0901234567");
    }
    String requestBody(String reason){return "{\"jobId\":\""+job+"\",\"reason\":\""+reason+"\"}";}
}
