package com.rikkei.jobs.profile.dto;

import com.rikkei.jobs.profile.entity.CandidateProfile;
import com.rikkei.jobs.profile.entity.CandidateProfile.Gender;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CandidateProfileResponse(UUID userId, String fullName, String email,
    LocalDate dob, Gender gender, String phone, String address, String avatarUrl,
    String headline, String summary, String program, String classCode, String courseName,
    String visibility, Instant updatedAt) {
    public static CandidateProfileResponse from(CandidateProfile p) {
        return new CandidateProfileResponse(p.getUserId(), p.getFullName(), p.getEmail(),
            p.getDob(), p.getGender(), p.getPhone(), p.getAddress(), p.getAvatarUrl(),
            p.getHeadline(), p.getSummary(), p.getProgram(), p.getClassCode(), p.getCourseName(),
            p.getVisibility(), p.getUpdatedAt());
    }
}
