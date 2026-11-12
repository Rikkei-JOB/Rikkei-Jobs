package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import org.springframework.stereotype.Component;
import static com.rikkei.jobs.recruitment.entity.ApplicationStatus.*;

@Component
public class ApplicationStatusPolicy {
    public boolean canWithdraw(ApplicationStatus status) {
        return status == SUBMITTED || status == SCREENING || status == INTERVIEW;
    }

    public void requireReapplicationAllowed(ApplicationStatus previous) {
        if (previous != REJECTED && previous != WITHDRAWN) {
            throw new ConflictException("Bạn đã có đơn ứng tuyển cho vị trí này.");
        }
    }

    public void requireTransition(ApplicationStatus from, ApplicationStatus to) {
        boolean next = switch (from) {
            case SUBMITTED -> to == SCREENING;
            case SCREENING -> to == INTERVIEW;
            case INTERVIEW -> to == OFFER;
            case OFFER -> to == HIRED;
            default -> false;
        };
        boolean reject = to == REJECTED && (canWithdraw(from) || from == OFFER);
        if (!next && !reject && !(to == WITHDRAWN && canWithdraw(from))) {
            throw new ConflictException("Không thể chuyển trạng thái từ " + from + " sang " + to + ".");
        }
    }
}
