package com.rikkei.jobs.recruitment.mapper;
import com.rikkei.jobs.recruitment.dto.response.*;
import com.rikkei.jobs.recruitment.entity.*;
import com.rikkei.jobs.recruitment.service.ApplicationStatusPolicy;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {
    private final ApplicationStatusPolicy policy;
    public ApplicationMapper(ApplicationStatusPolicy policy) { this.policy = policy; }
    public ApplicationSummaryResponse summary(Application a) {
        return new ApplicationSummaryResponse(a.getId(), a.getJobId(), a.getJobTitle(), a.getCompanyId(),
            a.getCompanyName(), a.getStatus(), a.getCreatedAt(), a.getUpdatedAt(), policy.canWithdraw(a.getStatus()));
    }
    public ApplicationDetailResponse detail(Application a, List<ApplicationStatusHistory> history) {
        return new ApplicationDetailResponse(a.getId(), a.getJobId(), a.getJobTitle(), a.getCompanyId(),
            a.getCompanyName(), a.getStatus(), a.getCreatedAt(), a.getUpdatedAt(), policy.canWithdraw(a.getStatus()),
            a.getCvId(), a.getCvSnapshotUrl(), a.getCoverLetter(), history.stream().map(h ->
                new ApplicationHistoryResponse(h.getId(), h.getFromStatus(), h.getToStatus(), h.getActorId(),
                    h.getToStatus() == ApplicationStatus.WITHDRAWN && a.getUserId().equals(h.getActorId()) ? h.getNote() : null,
                    h.getCreatedAt())).toList());
    }
}
