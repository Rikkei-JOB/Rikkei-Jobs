package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ForbiddenException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.dto.request.CreateOfferRequest;
import com.rikkei.jobs.recruitment.dto.response.OfferResponse;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.entity.Offer;
import com.rikkei.jobs.recruitment.entity.OfferStatus;
import com.rikkei.jobs.recruitment.repository.ApplicationRepository;
import com.rikkei.jobs.recruitment.repository.InterviewRoundRepository;
import com.rikkei.jobs.recruitment.repository.OfferRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class OfferCommandService {
    private final ApplicationRepository applications;
    private final OfferRepository offers;
    private final InterviewRoundRepository rounds;
    private final OfferEligibilityPolicy eligibility;
    private final ApplicationTransitionService transitions;
    private final Clock clock;

    @Transactional
    public OfferResponse send(AuthPrincipal actor, UUID applicationId, CreateOfferRequest request) {
        if (actor == null || actor.getRole() != Role.HR) throw new ForbiddenException("Chỉ HR được gửi offer.");
        var application = applications.findForUpdate(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển."));
        if (!application.getCompanyId().equals(actor.getCompanyId()))
            throw new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển.");
        if (application.getStatus() != ApplicationStatus.INTERVIEW || offers.findByApplicationId(applicationId).isPresent())
            throw new ConflictException("Đơn không ở trạng thái cho phép gửi offer.");
        eligibility.requireEligible(rounds.findByApplicationIdOrderByRoundNoAsc(applicationId));
        if (request == null || request.position() == null || request.position().isBlank()
            || request.workLocation() == null || request.workLocation().isBlank()
            || request.salary() == null || request.salary().signum() <= 0 || request.startDate() == null
            || request.responseDeadline() == null || !request.responseDeadline().toInstant().isAfter(clock.instant()))
            throw new BadRequestException("Thông tin offer không hợp lệ.");
        var offer = new Offer();
        offer.setId(UUID.randomUUID()); offer.setApplicationId(applicationId);
        offer.setPosition(request.position().strip()); offer.setSalary(request.salary().longValueExact());
        offer.setStartDate(request.startDate()); offer.setWorkLocation(request.workLocation().strip());
        offer.setNote(request.note() == null ? null : request.note().strip());
        offer.setResponseDeadline(request.responseDeadline().toInstant());
        offer.setStatus(OfferStatus.SENT); offer.setCreatedAt(clock.instant());
        offers.saveAndFlush(offer);
        transitions.transition(applicationId, ApplicationStatus.OFFER, actor.getUserId(), null);
        return OfferResponse.from(offer);
    }
}
