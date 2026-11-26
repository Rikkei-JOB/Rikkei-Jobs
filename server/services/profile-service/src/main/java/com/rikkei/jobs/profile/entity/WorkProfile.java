package com.rikkei.jobs.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Hồ sơ nghề nghiệp của người làm việc trên hệ thống (Mentor, Staff, Admin), bảng work_profiles (V14, đổi tên ở V15).
 * Họ tên, email thuộc identity (đồng bộ LMS) nên không lưu ở đây. Chuyên môn lưu một chuỗi, các mục cách nhau bởi xuống dòng.
 */
@Entity
@Table(name = "work_profiles")
@Getter @Setter @NoArgsConstructor
public class WorkProfile {
    @Id @Column(name = "user_id") private UUID userId;
    private String title;
    private String organization;
    @Column(name = "years_experience") private Integer yearsExperience;
    @Column(length = 10) private String phone;
    @Column(name = "linkedin_url", length = 500) private String linkedinUrl;
    @Column(length = 1000) private String expertise;
    @Column(length = 3000) private String bio;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
