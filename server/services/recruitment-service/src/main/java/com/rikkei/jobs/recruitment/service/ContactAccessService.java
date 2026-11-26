package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.recruitment.entity.*;
import com.rikkei.jobs.recruitment.repository.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class ContactAccessService {
    private final ApplicationRepository applications;
    private final ContactRequestRepository requests;
    public boolean canSeeContact(UUID companyId,UUID userId) { return accessVia(companyId,userId).isPresent(); }
    public Optional<ContactAccessVia> accessVia(UUID companyId,UUID userId) {
        if(companyId==null || userId==null) return Optional.empty();
        if(applications.existsByCompanyIdAndUserId(companyId,userId)) return Optional.of(ContactAccessVia.APPLICATION);
        return requests.existsByCompanyIdAndUserIdAndStatus(companyId,userId,ContactRequestStatus.APPROVED)
            ? Optional.of(ContactAccessVia.APPROVED_REQUEST) : Optional.empty();
    }
}
