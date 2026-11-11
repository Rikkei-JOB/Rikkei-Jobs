package com.rikkei.jobs.profile;

import com.rikkei.jobs.profile.dto.VerifiedSkillRecord;
import com.rikkei.jobs.profile.entity.MappingSource;
import com.rikkei.jobs.profile.repository.SkillScoreRepository;
import com.rikkei.jobs.profile.service.SkillProfileAccessService;
import com.rikkei.jobs.profile.service.SkillProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

// H2 REPEATABLE_READ does not implement PostgreSQL's transaction-wide MVCC snapshot.
@EnabledIfEnvironmentVariable(named = "HS01_TEST_DB_URL", matches = "jdbc:postgresql:.*")
@SpringBootTest(properties = {
        "spring.datasource.url=${HS01_TEST_DB_URL:jdbc:h2:mem:hs01_snapshot;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1}",
        "spring.datasource.username=${HS01_TEST_DB_USER:sa}",
        "spring.datasource.password=${HS01_TEST_DB_PASSWORD:}",
        "logging.level.root=WARN", "debug=false"
})
@Import(SkillProfileReadConsistencyTest.ScoreQueryPauseConfiguration.class)
class SkillProfileReadConsistencyTest {
    @Autowired SkillProfileService core;
    @Autowired SkillProfileAccessService access;
    @Autowired ScoreQueryPause pause;

    @Test
    void readReturnsOneSnapshotWhenRecomputeCommitsBetweenScoreAndEvidenceQueries() throws Exception {
        UUID user = UUID.randomUUID();
        core.importVerifiedRecords(user, List.of(record("80")));
        pause.user = user;

        ExecutorService reader = Executors.newSingleThreadExecutor(r -> new Thread(r, "hs01-snapshot-reader"));
        try {
            var pending = reader.submit(() -> access.getSkills(user, user));
            if (!pause.scoreRead.await(10, TimeUnit.SECONDS)) {
                pending.get(1, TimeUnit.SECONDS);
                throw new IllegalStateException("Reader did not reach the score query");
            }
            core.importVerifiedRecords(user, List.of(record("100")));
            pause.writeCommitted.countDown();
            var result = pending.get(10, TimeUnit.SECONDS).getFirst();
            assertThat(result.verifiedScore()).isEqualByComparingTo("80");
            assertThat(result.evidences()).singleElement()
                    .satisfies(e -> assertThat(e.score()).isEqualByComparingTo("80"));
            assertThat(core.getSkills(user).getFirst().verifiedScore()).isEqualByComparingTo("100");
        } finally {
            pause.writeCommitted.countDown();
            reader.shutdownNow();
        }
    }

    private VerifiedSkillRecord record(String score) {
        return new VerifiedSkillRecord(MappingSource.EXAM, "JAVA01", "snapshot-exam", "Thi Java",
                new BigDecimal(score), Instant.parse("2026-10-20T00:00:00Z"));
    }

    @TestConfiguration
    static class ScoreQueryPauseConfiguration {
        @Bean
        static ScoreQueryPause scoreQueryPause() {
            return new ScoreQueryPause();
        }
    }

    static class ScoreQueryPause implements BeanPostProcessor {
        private volatile UUID user;
        private final CountDownLatch scoreRead = new CountDownLatch(1);
        private final CountDownLatch writeCommitted = new CountDownLatch(1);

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) {
            if (!(bean instanceof SkillScoreRepository)) {
                return bean;
            }
            return Proxy.newProxyInstance(SkillScoreRepository.class.getClassLoader(),
                    new Class<?>[]{SkillScoreRepository.class}, (proxy, method, args) -> {
                        Object result;
                        try {
                            result = method.invoke(bean, args);
                        } catch (InvocationTargetException error) {
                            throw error.getCause();
                        }
                        if (method.getName().equals("findAllByIdUserId") && args[0].equals(user)
                                && Thread.currentThread().getName().equals("hs01-snapshot-reader")) {
                            scoreRead.countDown();
                            if (!writeCommitted.await(10, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Writer did not complete");
                            }
                        }
                        return result;
                    });
        }
    }
}
