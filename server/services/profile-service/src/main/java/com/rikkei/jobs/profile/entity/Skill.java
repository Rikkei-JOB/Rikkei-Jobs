package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "skills")
@Getter
@Setter
@NoArgsConstructor
public class Skill {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "skill_group", nullable = false, length = 100)
    private String group;

    /** NT-12: kỹ năng tắt không hiện ở danh mục chọn mới, nhưng bằng chứng và điểm cũ giữ nguyên. */
    @Column(nullable = false)
    private boolean active = true;
}
