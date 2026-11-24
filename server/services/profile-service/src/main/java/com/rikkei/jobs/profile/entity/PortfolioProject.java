package com.rikkei.jobs.profile.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Entity @Table(name = "portfolio_projects")
@Getter @Setter @NoArgsConstructor
public class PortfolioProject {
    public static final String IOC = "IOC";
    public static final String PERSONAL = "PERSONAL";
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, length = 16) private String source;
    @Column(name = "internship_id") private UUID internshipId;
    @Column(nullable = false) private String name;
    private String role;
    @Column(length = 5000) private String description;
    /** Các tag công nghệ nối bằng dấu phẩy; tag không được chứa dấu phẩy. */
    @Column(name = "tech_stack", length = 1100) private String techStack;
    @Column(name = "repo_url", length = 1000) private String repoUrl;
    @Column(name = "demo_url", length = 1000) private String demoUrl;
    @Column(name = "start_date") private LocalDate startDate;
    @Column(name = "end_date") private LocalDate endDate;
    @Column(nullable = false) private boolean visible;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    public boolean isIoc() { return IOC.equals(source); }
}
