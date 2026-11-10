package com.rikkei.jobs.identity.service.impl;

import com.rikkei.jobs.identity.config.exception.ApiException;
import com.rikkei.jobs.identity.config.exception.ErrorCode;
import com.rikkei.jobs.identity.entity.UsedSsoTicket;
import com.rikkei.jobs.identity.repository.UsedSsoTicketRepository;
import com.rikkei.jobs.identity.service.SsoTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class SsoTicketServiceImpl implements SsoTicketService {

    private final UsedSsoTicketRepository usedSsoTicketRepository;
    private final Clock clock;

    /**
     * Khoá chính của bảng đảm bảo tính nguyên tử: hai request cùng mang một vé thì chỉ một INSERT thành công.
     */
    @Override
    @Transactional
    public void consume(String ticket) {
        if (usedSsoTicketRepository.existsById(ticket)) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_USED);
        }
        try {
            usedSsoTicketRepository.saveAndFlush(new UsedSsoTicket(ticket, clock.instant()));
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorCode.TICKET_ALREADY_USED);
        }
    }
}
