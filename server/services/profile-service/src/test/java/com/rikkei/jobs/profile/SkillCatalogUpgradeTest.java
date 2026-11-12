package com.rikkei.jobs.profile;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class SkillCatalogUpgradeTest {
    @Test
    void upgradesAnExistingCatalogWithoutReplacingSkillIdsOrEvidence() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:catalog-upgrade;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(dataSource).schemas("profile").defaultSchema("profile")
                .locations("classpath:db/migration").target("3").load().migrate();
        var jdbc = new JdbcTemplate(dataSource);
        UUID skillId = UUID.fromString("00000000-0000-0000-0001-000000000014");
        assertThat(jdbc.queryForObject("select code from profile.skills where id = ?", String.class, skillId))
                .isEqualTo("DATA_STRUCTURES_ALGORITHMS");
        UUID evidenceId = UUID.randomUUID();
        jdbc.update("insert into profile.skill_evidences "
                        + "(id, user_id, skill_id, source, source_ref, title, score, weight, recorded_at, verified) "
                        + "values (?, ?, ?, 'SELF', 'upgrade-test', 'DSA evidence', 70, 1, CURRENT_TIMESTAMP, FALSE)",
                evidenceId, UUID.randomUUID(), skillId);

        Flyway.configure().dataSource(dataSource).schemas("profile").defaultSchema("profile")
                .locations("classpath:db/migration").load().migrate();

        assertThat(jdbc.queryForObject("select code from profile.skills where id = ?", String.class, skillId))
                .isEqualTo("DSA");
        assertThat(jdbc.queryForObject("select skill_id from profile.skill_evidences where id = ?", UUID.class, evidenceId))
                .isEqualTo(skillId);
        assertThat(jdbc.queryForObject("select count(*) from profile.skills", Integer.class)).isEqualTo(20);
    }
}
