package com.rikkei.jobs.company.service;

import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.company.dto.response.MatchResponse;
import java.util.UUID;

public interface MatchService {

    /** Ứng viên xem điểm của chính mình; chỉ JD APPROVED, ngược lại 404. */
    MatchResponse forCandidate(UUID jobId, UUID candidateId, String authorization);

    /** HR (JD của công ty và ứng viên đã ứng tuyển) hoặc STAFF (mọi JD) xem điểm của một ứng viên. */
    MatchResponse forViewer(UUID jobId, UUID userId, AuthPrincipal viewer, String authorization);
}
