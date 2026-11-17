package com.rikkei.jobs.recruitment.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateRoundRequest(@NotBlank @Size(max = 255) String name,
    @NotBlank @Email @Size(max = 320) String interviewerEmail, Instant scheduledAt) {}
