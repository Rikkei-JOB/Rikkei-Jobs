package com.rikkei.jobs.profile;

import com.rikkei.jobs.profile.entity.Skill;
import com.rikkei.jobs.profile.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=${HS01_TEST_DB_URL:jdbc:h2:mem:hs01_catalog;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${HS01_TEST_DB_USER:sa}",
        "spring.datasource.password=${HS01_TEST_DB_PASSWORD:}",
        "app.jwt.secret=test_secret_test_secret_test_secret_test_secret",
        "spring.profiles.active=test",
        "spring.jpa.show-sql=false",
        "logging.level.root=WARN"
})
class MentorSkillCatalogTest {

    @Autowired
    private SkillRepository skillRepository;

    private static final Map<String, String> EXPECTED_MENTOR_SKILLS = Map.ofEntries(
            Map.entry("JAVA", "Java"),
            Map.entry("SPRING_BOOT", "Spring Boot"),
            Map.entry("SQL", "SQL"),
            Map.entry("GIT", "Git"),
            Map.entry("REST_API", "REST API"),
            Map.entry("HTML_CSS", "HTML/CSS"),
            Map.entry("JAVASCRIPT", "JavaScript"),
            Map.entry("REACTJS", "ReactJS"),
            Map.entry("DOCKER", "Docker"),
            Map.entry("ENGLISH", "Tiếng Anh"),
            Map.entry("TESTING", "Kiểm thử phần mềm"),
            Map.entry("LINUX", "Linux"),
            Map.entry("OOP", "Lập trình hướng đối tượng"),
            Map.entry("DSA", "Cấu trúc dữ liệu và giải thuật"),
            Map.entry("CLEAN_CODE", "Clean code"),
            Map.entry("TEAMWORK", "Làm việc nhóm"),
            Map.entry("COMMUNICATION", "Giao tiếp"),
            Map.entry("AGILE", "Agile/Scrum"),
            Map.entry("SECURITY", "Bảo mật"),
            Map.entry("CLOUD", "Điện toán đám mây")
    );

    @Test
    void catalogMatchesExactTwentyMentorSkillsFromConventionsDoc() {
        var allSkills = skillRepository.findAll();
        assertThat(allSkills).hasSize(20);

        Map<String, String> actualMap = allSkills.stream()
                .collect(Collectors.toMap(Skill::getCode, Skill::getName));

        assertThat(actualMap).containsExactlyInAnyOrderEntriesOf(EXPECTED_MENTOR_SKILLS);

        // Ensure none of the old draft codes remain
        assertThat(actualMap).doesNotContainKeys(
                "UNIT_TESTING",
                "DATA_STRUCTURES_ALGORITHMS",
                "TYPESCRIPT",
                "NEXTJS",
                "CI_CD",
                "PROBLEM_SOLVING"
        );
    }
}
