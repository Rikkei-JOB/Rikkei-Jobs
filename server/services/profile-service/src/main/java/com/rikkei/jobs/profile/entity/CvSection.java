package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.io.Serializable;
import java.util.UUID;

@Entity @Table(name = "cv_sections") @IdClass(CvSection.Key.class)
@Getter @Setter @NoArgsConstructor
public class CvSection {
    /** Thứ tự khai báo là thứ tự mặc định khi tạo CV mới. */
    public enum Type { SUMMARY, SKILLS, LANGUAGES, INTERNSHIP, PROJECTS, EDUCATION, CERTIFICATES }

    @Id @Column(name = "cv_id") private UUID cvId;
    @Id @Enumerated(EnumType.STRING) @Column(length = 16) private Type section;
    /** Mảng JSON id mục được chọn; null là lấy mọi mục. */
    @Column(name = "item_ids", length = 8000) private String itemIds;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @Column(nullable = false) private boolean included;

    @Getter @Setter @NoArgsConstructor @lombok.AllArgsConstructor @lombok.EqualsAndHashCode
    public static class Key implements Serializable {
        private UUID cvId;
        private Type section;
    }
}
