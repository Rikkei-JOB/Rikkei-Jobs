package com.rikkei.jobs.recruitment.dto.request;

import com.rikkei.jobs.recruitment.entity.InterviewRoundResult;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateRoundRequest(@NotNull InterviewRoundResult result,
    @DecimalMin("0") @DecimalMax("100") BigDecimal score, @Size(max = 2000) String note) {}
