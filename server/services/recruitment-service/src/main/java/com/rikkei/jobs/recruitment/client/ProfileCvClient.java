package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.recruitment.dto.integration.CvPdf;
import java.util.UUID;

public interface ProfileCvClient {
    CvPdf exportOwnedPdf(UUID cvId, String candidateBearerToken);
}
