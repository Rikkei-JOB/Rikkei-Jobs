package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.recruitment.config.InvalidTransitionException;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import static com.rikkei.jobs.recruitment.entity.ApplicationStatus.*;

@Component
public class HrTransitionPolicy {
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> ALLOWED = Map.of(
        SUBMITTED, Set.of(SCREENING, REJECTED),
        SCREENING, Set.of(INTERVIEW, REJECTED),
        INTERVIEW, Set.of(REJECTED));

    public Set<ApplicationStatus> allowedFrom(ApplicationStatus from) {
        return ALLOWED.getOrDefault(from, Set.of());
    }

    public void requireAllowed(ApplicationStatus from, ApplicationStatus to) {
        if (!allowedFrom(from).contains(to)) {
            throw new InvalidTransitionException("Không thể chuyển trạng thái từ " + from + " sang " + to + ".");
        }
    }
}
