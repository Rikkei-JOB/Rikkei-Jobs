package com.rikkei.jobs.profile;

import com.rikkei.jobs.profile.config.ProfileDemoDataSeeder;
import com.rikkei.jobs.profile.entity.EvidenceSource;
import com.rikkei.jobs.profile.repository.*;
import com.rikkei.jobs.profile.service.SkillProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@ActiveProfiles("dev")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:hs01_demo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.sql.init.mode=always", "spring.sql.init.schema-locations=classpath:hs01/identity-demo.sql",
        "app.profile.demo-data-enabled=true", "logging.level.root=WARN", "debug=false"
})
class ProfileDemoDataSeederTest {
    @Autowired ProfileDemoDataSeeder seeder;
    @Autowired SkillProfileService core;
    @Autowired ExternalSkillRecordRepository external;
    @Autowired SkillEvidenceRepository evidences;
    @Autowired SkillProfileStateRepository states;
    @Autowired SkillScoreRepository scores;

    @Test
    void seedsRealIdentityIdsAndSelfOnlySkillWithoutPublishingOrDuplicating() {
        UUID realCandidate = UUID.fromString("20000000-0000-0000-0000-000000000001");
        var skills = core.getSkills(realCandidate);
        assertThat(skills).hasSize(9);
        assertThat(skills).filteredOn(v -> v.skill().code().equals("ENGLISH")).singleElement().satisfies(v -> {
            assertThat(v.verifiedScore()).isNull();
            assertThat(v.level()).isNull();
            assertThat(v.selfScore()).isEqualByComparingTo("65");
            assertThat(v.evidences()).singleElement().satisfies(e -> {
                assertThat(e.source()).isEqualTo(EvidenceSource.SELF);
                assertThat(e.verified()).isFalse();
            });
        });
        assertThat(core.getSkills(UUID.fromString("10000000-0000-0000-0000-000000000001"))).isEmpty();
        long sourceCount = external.count();
        long evidenceCount = evidences.count();
        seeder.run(null);
        assertThat(external.count()).isEqualTo(sourceCount);
        assertThat(evidences.count()).isEqualTo(evidenceCount);
        assertThat(states.findAll()).hasSize(3).allSatisfy(state -> {
            assertThat(state.isConsented()).isFalse();
            assertThat(state.isApproved()).isFalse();
        });
        // A user removed the demo self skill; a later dev startup must not restore it.
        var english = skills.stream().filter(v -> v.skill().code().equals("ENGLISH")).findFirst().orElseThrow();
        evidences.deleteAll(evidences.findAllByUserId(realCandidate).stream()
                .filter(e -> e.getSource() == EvidenceSource.SELF).toList());
        scores.deleteById(new com.rikkei.jobs.profile.entity.SkillScoreId(realCandidate, english.skill().id()));
        seeder.run(null);
        assertThat(core.getSkills(realCandidate)).noneMatch(v -> v.skill().code().equals("ENGLISH"));
    }
}
