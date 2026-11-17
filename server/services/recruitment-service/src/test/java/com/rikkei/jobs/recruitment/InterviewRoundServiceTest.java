package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.recruitment.dto.request.CreateRoundRequest;
import com.rikkei.jobs.recruitment.dto.request.UpdateRoundRequest;
import com.rikkei.jobs.recruitment.entity.*;
import com.rikkei.jobs.recruitment.repository.*;
import com.rikkei.jobs.recruitment.service.InterviewRoundService;
import com.rikkei.jobs.recruitment.client.MentorIdentityClient;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InterviewRoundServiceTest {
    private final ApplicationRepository applications = mock(ApplicationRepository.class);
    private final InterviewRoundRepository rounds = mock(InterviewRoundRepository.class);
    private final InterviewRoundRevisionRepository revisions = mock(InterviewRoundRevisionRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final InterviewRoundService service = new InterviewRoundService(applications, rounds, revisions, clock,
        mock(MentorIdentityClient.class));
    private final UUID companyId = UUID.randomUUID(), applicationId = UUID.randomUUID(), actorId = UUID.randomUUID();
    private final AuthPrincipal hr = new AuthPrincipal(actorId, Role.HR, companyId);
    private Application application;

    @BeforeEach void setUp() {
        application = new Application();
        application.setId(applicationId);
        application.setCompanyId(companyId);
        application.setStatus(ApplicationStatus.INTERVIEW);
        when(applications.findForUpdate(applicationId)).thenReturn(Optional.of(application));
        when(rounds.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void roundsCanOnlyBeCreatedDuringInterview() {
        application.setStatus(ApplicationStatus.SCREENING);
        assertThatThrownBy(() -> service.create(hr, applicationId,
            new CreateRoundRequest("Kỹ thuật", "interviewer@example.test", null)))
            .isInstanceOf(ConflictException.class);
        verify(rounds, never()).saveAndFlush(any());
    }

    @Test void creatingRoundsAssignsSequentialNumbersAndPreservesOptionalSchedule() {
        when(rounds.maxRoundNo(applicationId)).thenReturn(0, 1);
        var first = service.create(hr, applicationId,
            new CreateRoundRequest("Kỹ thuật", "interviewer@example.test", null));
        var schedule = Instant.parse("2030-01-03T08:00:00Z");
        var second = service.create(hr, applicationId,
            new CreateRoundRequest("Văn hóa", "interviewer@example.test", schedule));
        assertThat(first.roundNo()).isEqualTo(1);
        assertThat(first.result()).isEqualTo(InterviewRoundResult.PENDING);
        assertThat(first.scheduledAt()).isNull();
        assertThat(second.roundNo()).isEqualTo(2);
        assertThat(second.scheduledAt()).isEqualTo(schedule);
    }

    @Test void changedResultIsAuditedWithOldAndNewValues() {
        var roundId = UUID.randomUUID();
        var round = existing(roundId);
        when(rounds.findByIdAndApplicationId(roundId, applicationId)).thenReturn(Optional.of(round));
        service.update(hr, applicationId, roundId,
            new UpdateRoundRequest(InterviewRoundResult.PASS, new BigDecimal("85"), "Đạt"));
        service.update(hr, applicationId, roundId,
            new UpdateRoundRequest(InterviewRoundResult.FAIL, new BigDecimal("40"), "Sửa kết quả"));
        var saved = ArgumentCaptor.forClass(InterviewRoundRevision.class);
        verify(revisions, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getOldResult()).isEqualTo(InterviewRoundResult.PENDING);
        assertThat(saved.getAllValues().get(0).getNewResult()).isEqualTo(InterviewRoundResult.PASS);
        assertThat(saved.getAllValues().get(1).getOldResult()).isEqualTo(InterviewRoundResult.PASS);
        assertThat(saved.getAllValues().get(1).getNewResult()).isEqualTo(InterviewRoundResult.FAIL);
        assertThat(saved.getAllValues()).allSatisfy(revision -> assertThat(revision.getActorId()).isEqualTo(actorId));
    }

    @Test void updateRejectsRoundFromAnotherApplication() {
        var roundId = UUID.randomUUID();
        when(rounds.findByIdAndApplicationId(roundId, applicationId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(hr, applicationId, roundId,
            new UpdateRoundRequest(InterviewRoundResult.PASS, null, null)))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void editingAfterInterviewIsBlocked() {
        application.setStatus(ApplicationStatus.OFFER);
        assertThatThrownBy(() -> service.update(hr, applicationId, UUID.randomUUID(),
            new UpdateRoundRequest(InterviewRoundResult.PASS, null, null)))
            .isInstanceOf(ConflictException.class);
    }

    @Test void pendingResultClearsPreviousScore() {
        var roundId = UUID.randomUUID();
        var round = existing(roundId);
        round.setResult(InterviewRoundResult.PASS);
        round.setScore(new BigDecimal("90"));
        when(rounds.findByIdAndApplicationId(roundId, applicationId)).thenReturn(Optional.of(round));
        var changed = service.update(hr, applicationId, roundId,
            new UpdateRoundRequest(InterviewRoundResult.PENDING, new BigDecimal("80"), null));
        assertThat(changed.score()).isNull();
    }

    private InterviewRound existing(UUID id) {
        var round = new InterviewRound();
        round.setId(id);
        round.setApplicationId(applicationId);
        round.setRoundNo(1);
        round.setName("Kỹ thuật");
        round.setInterviewerEmail("interviewer@example.test");
        round.setResult(InterviewRoundResult.PENDING);
        round.setCreatedAt(clock.instant());
        return round;
    }
}
