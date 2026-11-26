package com.rikkei.jobs.profile.dto;

import java.util.List;
import java.util.UUID;

public record StaffProfilePreview(UUID userId, String fullName, String headline, String summary,
                                 String classCode, String courseName, List<SkillProfileView> skills) {}
