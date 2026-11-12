package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.security.principle.*;
import com.rikkei.jobs.recruitment.dto.request.*;
import com.rikkei.jobs.recruitment.entity.ApplicationStatus;
import com.rikkei.jobs.recruitment.service.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.*;

class ApplicationTransitionConcurrencyTest extends ApplicationIntegrationSupport {
    @Autowired ApplicationService service;
    @Autowired ApplicationTransitionService transitions;

    private List<Integer> race(Callable<Integer> first, Callable<Integer> second) throws Exception {
        try (var pool = Executors.newFixedThreadPool(2)) {
            var ready = new CountDownLatch(2);
            var start = new CountDownLatch(1);
            Callable<Integer> wrapFirst = () -> { ready.countDown(); start.await(); return first.call(); };
            Callable<Integer> wrapSecond = () -> { ready.countDown(); start.await(); return second.call(); };
            var a = pool.submit(wrapFirst); var b = pool.submit(wrapSecond);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }
    private int submit() {
        try { service.apply(new AuthPrincipal(user, Role.CANDIDATE, null), candidate(), job, new ApplyRequest(cv, null)); return 201; }
        catch (ConflictException ex) { return 409; }
    }
    private Set<String> snapshotFiles() throws java.io.IOException {
        var path = java.nio.file.Path.of("target/ut01-snapshots");
        if (!java.nio.file.Files.exists(path)) return Set.of();
        try (var files = java.nio.file.Files.list(path)) {
            return files.filter(p -> p.toString().endsWith(".pdf")).map(p -> p.getFileName().toString()).collect(java.util.stream.Collectors.toSet());
        }
    }
    @Test void simultaneousSubmissionsCreateOneApplication() throws Exception {
        var before = snapshotFiles();
        assertThat(race(this::submit, this::submit)).containsExactlyInAnyOrder(201,409);
        assertThat(count("applications")).isEqualTo(1);
        assertThat(count("application_status_history")).isEqualTo(1);
        assertThat(count("application_notification_outbox")).isEqualTo(1);
        var added = new HashSet<>(snapshotFiles()); added.removeAll(before);
        assertThat(added).hasSize(1);
    }
    @Test void simultaneousReapplicationsAfterRejectedCreateOnlyOneNewAttempt() throws Exception {
        var old = apply();
        transitions.transition(old, ApplicationStatus.REJECTED, UUID.randomUUID(), "Internal evaluation");
        var before = snapshotFiles();
        assertThat(race(this::submit, this::submit)).containsExactlyInAnyOrder(201,409);
        assertThat(count("applications")).isEqualTo(2);
        assertThat(count("application_status_history")).isEqualTo(3);
        assertThat(count("application_notification_outbox")).isEqualTo(2);
        assertThat(jdbc.queryForObject("select status from recruitment.applications where id=?", String.class, old)).isEqualTo("REJECTED");
        var added = new HashSet<>(snapshotFiles()); added.removeAll(before);
        assertThat(added).hasSize(1);
    }
    @Test void offerAndWithdrawalSerializeWithoutLostHistory() throws Exception {
        var id = apply();
        transitions.transition(id, ApplicationStatus.SCREENING, UUID.randomUUID(), null);
        transitions.transition(id, ApplicationStatus.INTERVIEW, UUID.randomUUID(), null);
        var results = race(() -> {
            try { transitions.transition(id, ApplicationStatus.OFFER, UUID.randomUUID(), null); return 200; }
            catch (ConflictException ex) { return 409; }
        }, () -> {
            try { service.withdraw(new AuthPrincipal(user, Role.CANDIDATE, null), id, new WithdrawRequest("Changed plans")); return 200; }
            catch (ConflictException ex) { return 409; }
        });
        assertThat(results).containsExactlyInAnyOrder(200,409);
        assertThat(count("application_status_history")).isEqualTo(4);
        assertThat(jdbc.queryForObject("select status from recruitment.applications where id=?", String.class, id)).isIn("OFFER","WITHDRAWN");
    }
}
