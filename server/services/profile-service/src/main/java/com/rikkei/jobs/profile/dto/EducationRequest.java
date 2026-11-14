package com.rikkei.jobs.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EducationRequest(@NotBlank @Size(max = 255) String school,
    @Size(max = 255) String major, @NotNull LocalDate fromDate, LocalDate toDate,
    @DecimalMin("0") @DecimalMax("10") BigDecimal gpa, @Size(max = 5000) String note) {
    public EducationRequest {
        school = trim(school); major = trim(major); note = trim(note);
    }
    @JsonIgnore @AssertTrue(message = "Ngày kết thúc phải bằng hoặc sau ngày bắt đầu")
    public boolean isDateRangeValid() {
        return fromDate == null || toDate == null || !toDate.isBefore(fromDate);
    }
    private static String trim(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
