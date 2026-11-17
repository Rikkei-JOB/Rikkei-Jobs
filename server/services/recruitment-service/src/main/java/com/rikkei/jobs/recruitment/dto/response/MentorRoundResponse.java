package com.rikkei.jobs.recruitment.dto.response;

import com.rikkei.jobs.recruitment.entity.ApplicationStatus;

public record MentorRoundResponse(String jobTitle, String companyName,
    ApplicationStatus applicationStatus, InterviewRoundResponse round) {}
