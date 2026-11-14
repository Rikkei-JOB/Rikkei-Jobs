package com.rikkei.jobs.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rikkei.jobs.profile.entity.CandidateProfile.Gender;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateCandidateProfileRequest(
    @NotBlank @Size(max = 255) String fullName,
    @NotBlank @Email @Size(max = 255) String email,
    @PastOrPresent LocalDate dob,
    Gender gender,
    @Pattern(regexp = "0[0-9]{9}") String phone,
    @Size(max = 500) String address,
    @Size(max = 255) String headline,
    @Size(max = 5000) String summary
) {
    public UpdateCandidateProfileRequest {
        fullName = trim(fullName); email = trim(email); phone = trim(phone);
        address = trim(address); headline = trim(headline); summary = trim(summary);
    }
    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
