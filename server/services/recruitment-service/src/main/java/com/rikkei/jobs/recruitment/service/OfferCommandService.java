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
import com.rikkei.jobs.recruitment.entity.EmploymentRecord;
import com.rikkei.jobs.recruitment.entity.Offer;
import com.rikkei.jobs.recruitment.entity.OfferStatus;
import com.rikkei.jobs.recruitment.repository.ApplicationRepository;
import com.rikkei.jobs.recruitment.repository.EmploymentRecordRepository;
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
    private final EmploymentRecordRepository employmentRecords;
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

    @Transactional
    public OfferResponse accept(AuthPrincipal actor, UUID offerId) {
        var locked = lockOwned(actor, offerId);
        requireOpen(locked.offer());
        locked.offer().setStatus(OfferStatus.ACCEPTED);
        locked.offer().setRespondedAt(clock.instant());
        offers.saveAndFlush(locked.offer());
        transitions.transition(locked.application().getId(), ApplicationStatus.HIRED, actor.getUserId(), null);
        var record = new EmploymentRecord();
        record.setId(UUID.randomUUID()); record.setApplicationId(locked.application().getId());
        record.setUserId(locked.application().getUserId()); record.setCompanyId(locked.application().getCompanyId());
        record.setJobId(locked.application().getJobId()); record.setPosition(locked.offer().getPosition());
        record.setStartDate(locked.offer().getStartDate());
        employmentRecords.saveAndFlush(record);
        return OfferResponse.from(locked.offer());
    }

    @Transactional
    public OfferResponse decline(AuthPrincipal actor, UUID offerId, String reason) {
        var locked = lockOwned(actor, offerId);
        requireOpen(locked.offer());
        String cleaned = reason == null ? null : reason.strip();
        if (cleaned == null || cleaned.isBlank() || cleaned.length() > 1000)
            throw new BadRequestException("Lý do từ chối phải từ 1 đến 1.000 ký tự.");
        locked.offer().setStatus(OfferStatus.DECLINED);
        locked.offer().setRespondedAt(clock.instant());
        locked.offer().setDeclineReason(cleaned);
        offers.saveAndFlush(locked.offer());
        transitions.transition(locked.application().getId(), ApplicationStatus.REJECTED, actor.getUserId(), null);
        return OfferResponse.from(locked.offer());
    }

    private record Locked(com.rikkei.jobs.recruitment.entity.Application application, Offer offer) {}

    private Locked lockOwned(AuthPrincipal actor, UUID offerId) {
        if (actor == null || actor.getRole() != Role.CANDIDATE)
            throw new ForbiddenException("Chỉ ứng viên được phản hồi offer.");
        var visible = offers.findById(offerId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy offer."));
        var application = applications.findForUpdate(visible.getApplicationId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy offer."));
        if (!application.getUserId().equals(actor.getUserId()))
            throw new ResourceNotFoundException("Không tìm thấy offer.");
        var offer = offers.findByApplicationIdForUpdate(application.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy offer."));
        if (!offer.getId().equals(offerId)) throw new ResourceNotFoundException("Không tìm thấy offer.");
        return new Locked(application, offer);
    }

    private void requireOpen(Offer offer) {
        if (offer.getStatus() != OfferStatus.SENT || !clock.instant().isBefore(offer.getResponseDeadline()))
            throw new ConflictException("Offer không còn hiệu lực để phản hồi.");
    }
}
