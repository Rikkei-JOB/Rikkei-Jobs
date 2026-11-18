package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.recruitment.dto.response.OfferResponse;
import com.rikkei.jobs.recruitment.repository.ApplicationRepository;
import com.rikkei.jobs.recruitment.repository.OfferRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class OfferQueryService {
    private final OfferRepository offers;
    private final ApplicationRepository applications;

    @Transactional(readOnly = true)
    public PageResponse<OfferResponse> listOwn(UUID userId, int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new BadRequestException("page/size không hợp lệ.");
        var result = offers.findByOwner(userId, PageRequest.of(page - 1, size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return PageResponse.of(result, result.getContent().stream().map(OfferResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public OfferResponse getOwn(UUID userId, UUID offerId) {
        var offer = offers.findById(offerId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy offer."));
        var application = applications.findById(offer.getApplicationId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy offer."));
        if (!application.getUserId().equals(userId)) throw new ResourceNotFoundException("Không tìm thấy offer.");
        return OfferResponse.from(offer);
    }
}
