package com.rikkei.jobs.profile.dto;

import com.rikkei.jobs.profile.entity.Education;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record EducationResponse(UUID id, String school, String major, LocalDate fromDate,
    LocalDate toDate, BigDecimal gpa, String note) {
    public static EducationResponse from(Education e) {
        return new EducationResponse(e.getId(), e.getSchool(), e.getMajor(), e.getFromDate(), e.getToDate(), e.getGpa(), e.getNote());
    }
}
