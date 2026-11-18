package com.rikkei.jobs.recruitment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record CreateOfferRequest(
    @NotBlank @Size(max = 255) String position,
    @NotNull @Positive @Digits(integer = 18, fraction = 0) BigDecimal salary,
    @NotNull LocalDate startDate,
    @NotBlank @Size(max = 500) String workLocation,
    @NotNull OffsetDateTime responseDeadline,
    @Size(max = 2000) String note
) {}
