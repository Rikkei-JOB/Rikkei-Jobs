package com.rikkei.jobs.recruitment.dto.response;

import com.rikkei.jobs.recruitment.entity.Offer;
import com.rikkei.jobs.recruitment.entity.OfferStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OfferResponse(UUID id, UUID applicationId, String position, Long salary,
    LocalDate startDate, String workLocation, String note, Instant responseDeadline,
    OfferStatus status, Instant respondedAt, String declineReason, Instant createdAt) {
    public static OfferResponse from(Offer offer) {
        return new OfferResponse(offer.getId(), offer.getApplicationId(), offer.getPosition(),
            offer.getSalary(), offer.getStartDate(), offer.getWorkLocation(), offer.getNote(),
            offer.getResponseDeadline(), offer.getStatus(), offer.getRespondedAt(),
            offer.getDeclineReason(), offer.getCreatedAt());
    }
}
