package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.service.ContactAccessService;
import com.rikkei.jobs.recruitment.repository.*;
import com.rikkei.jobs.recruitment.entity.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class ContactAccessPolicyTest {
    ApplicationRepository applications = mock(ApplicationRepository.class);
    ContactRequestRepository requests = mock(ContactRequestRepository.class);
    ContactAccessService access = new ContactAccessService(applications, requests);
    UUID company = UUID.randomUUID(), user = UUID.randomUUID();

    @Test void applicationTakesPrecedence() {
        when(applications.existsByCompanyIdAndUserId(company,user)).thenReturn(true);
        assertThat(access.accessVia(company,user)).contains(ContactAccessVia.APPLICATION);
    }
    @Test void approvedRequestGrantsAccess() {
        when(requests.existsByCompanyIdAndUserIdAndStatus(company,user,ContactRequestStatus.APPROVED)).thenReturn(true);
        assertThat(access.accessVia(company,user)).contains(ContactAccessVia.APPROVED_REQUEST);
    }
    @Test void anotherCompanyDenied() {
        assertThat(access.canSeeContact(UUID.randomUUID(),user)).isFalse();
        assertThat(access.canSeeContact(company,user)).isFalse();
    }
}
