package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "candidate_profiles")
@Getter @Setter @NoArgsConstructor
public class CandidateProfile {
    public enum Gender { MALE, FEMALE, OTHER }
    @Id @Column(name = "user_id") private UUID userId;
    @Column(name = "full_name", nullable = false) private String fullName;
    private LocalDate dob;
    @Enumerated(EnumType.STRING) @Column(length = 10) private Gender gender;
    @Column(length = 10) private String phone;
    @Column(nullable = false) private String email;
    @Column(length = 500) private String address;
    @Column(name = "avatar_url", length = 1000) private String avatarUrl;
    private String headline;
    @Column(length = 5000) private String summary;
    @Column(nullable = false) private String program;
    @Column(name = "class_code", nullable = false, length = 100) private String classCode;
    @Column(name = "course_name", nullable = false) private String courseName;
    @Column(nullable = false, length = 10) private String visibility = "PRIVATE";
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
