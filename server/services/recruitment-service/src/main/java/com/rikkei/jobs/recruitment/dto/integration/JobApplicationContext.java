package com.rikkei.jobs.recruitment.dto.integration;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobApplicationContext(UUID jobId, UUID companyId, String jobTitle, String companyName,
    String status, Instant deadline, Boolean companyLocked, List<String> hrEmails) {}
