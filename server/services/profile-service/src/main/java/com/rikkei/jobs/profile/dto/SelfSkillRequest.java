package com.rikkei.jobs.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rikkei.jobs.common.config.exception.BadRequestException;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SelfSkillRequest(@NotNull UUID skillId,
    @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer = 3, fraction = 6) BigDecimal selfScore) {
    public SelfSkillRequest {
        if (skillId == null || selfScore == null || selfScore.signum() < 0
            || selfScore.compareTo(BigDecimal.valueOf(100)) > 0 || selfScore.stripTrailingZeros().scale() > 6)
            throw new BadRequestException("Kỹ năng và điểm tự khai 0–100 là bắt buộc; điểm tối đa 6 chữ số thập phân.");
    }
}
