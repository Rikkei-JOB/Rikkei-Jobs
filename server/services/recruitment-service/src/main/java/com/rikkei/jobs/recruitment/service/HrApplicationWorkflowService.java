package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.common.config.exception.ForbiddenException;
import com.rikkei.jobs.recruitment.dto.response.ApplicationWorkflowResponse;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.repository.ApplicationRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class HrApplicationWorkflowService {
    private final ApplicationRepository applications;
    private final HrTransitionPolicy policy;
    private final ApplicationTransitionService transitions;

    @Transactional
    public ApplicationWorkflowResponse transition(AuthPrincipal actor, UUID id, ApplicationStatus to, String note) {
        if (actor == null || actor.getRole() != Role.HR) throw new ForbiddenException("Chỉ HR được chuyển bước đơn.");
        var application = applications.findForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển."));
        if (!application.getCompanyId().equals(actor.getCompanyId()))
            throw new ResourceNotFoundException("Không tìm thấy đơn ứng tuyển.");
        policy.requireAllowed(application.getStatus(), to);
        var updated = transitions.transition(id, to, actor.getUserId(), note);
        return new ApplicationWorkflowResponse(updated.getId(), updated.getStatus(), updated.getUpdatedAt());
    }
}
