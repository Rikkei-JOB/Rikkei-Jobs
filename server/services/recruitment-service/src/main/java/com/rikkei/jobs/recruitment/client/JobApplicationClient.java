package com.rikkei.jobs.recruitment.client;

import com.rikkei.jobs.recruitment.dto.integration.JobApplicationContext;
import java.util.UUID;

public interface JobApplicationClient {
    JobApplicationContext getContext(UUID jobId);
}
