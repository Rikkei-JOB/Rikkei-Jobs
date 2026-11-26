package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.recruitment.client.*;
import com.rikkei.jobs.recruitment.config.ContactException;
import com.rikkei.jobs.recruitment.dto.contact.ContactDtos.ContactView;
import com.rikkei.jobs.recruitment.entity.ContactAccessLog;
import com.rikkei.jobs.recruitment.repository.ContactAccessLogRepository;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import java.util.UUID;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor
public class CandidateContactService {
    private final ContactAccessService access;
    private final IdentityCandidateClient identities;
    private final CandidateContactClient contacts;
    private final ContactAccessLogRepository logs;
    private final Clock clock;
    /** Spring's transaction proxy commits the audit before the controller can receive the contact. */
    @Transactional
    public ContactView reveal(AuthPrincipal actor,UUID userId,String bearer) {
        ContactRequestService.requireHr(actor);
        var via=access.accessVia(actor.getCompanyId(),userId).orElseThrow(()->
            new ContactException(403,"CONTACT_LOCKED","Liên hệ chưa được mở."));
        var identity=identities.get(userId);
        String phone=contacts.phone(userId,bearer);
        var log=new ContactAccessLog();log.setId(UUID.randomUUID());log.setCompanyId(actor.getCompanyId());
        log.setHrId(actor.getUserId());log.setUserId(userId);log.setVia(via);log.setCreatedAt(clock.instant());
        logs.save(log);
        return new ContactView(identity.email(),phone);
    }
}
