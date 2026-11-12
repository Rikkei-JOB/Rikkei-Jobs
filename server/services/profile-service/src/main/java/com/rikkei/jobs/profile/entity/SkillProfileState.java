package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "skill_profile_states")
@Getter
@Setter
@NoArgsConstructor
public class SkillProfileState {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false)
    private boolean consented;

    @Column(nullable = false)
    private boolean approved;
}
