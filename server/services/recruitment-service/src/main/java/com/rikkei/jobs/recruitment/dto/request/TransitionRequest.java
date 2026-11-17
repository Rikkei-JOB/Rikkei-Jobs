package com.rikkei.jobs.recruitment.dto.request;

import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransitionRequest(@NotNull ApplicationStatus toStatus, @Size(max = 1000) String note) {}
