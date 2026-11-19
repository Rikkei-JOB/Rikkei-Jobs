package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.company.client.ApplicationClient;
import com.rikkei.jobs.company.client.CatalogSkill;
import com.rikkei.jobs.company.client.ProfileSkillClient;
import com.rikkei.jobs.company.client.SkillCatalogClient;
import com.rikkei.jobs.company.dto.response.MatchResponse;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.MatchScore;
import com.rikkei.jobs.company.repository.JobRepository;
import com.rikkei.jobs.company.repository.JobSkillRepository;
import com.rikkei.jobs.company.repository.MatchScoreRepository;
import com.rikkei.jobs.company.service.CompanyService;
import com.rikkei.jobs.company.service.impl.MatchServiceImpl;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class MatchServiceImplTest {

    private static final UUID JAVA = UUID.fromString("00000000-0000-0000-0001-000000000001");

    @Test
    void insertConflictStillReturnsResult() {
        UUID jobId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        JobRepository jobRepository = mock(JobRepository.class);
        JobSkillRepository jobSkillRepository = mock(JobSkillRepository.class);
        MatchScoreRepository matchScoreRepository = mock(MatchScoreRepository.class);
        SkillCatalogClient skillCatalog = mock(SkillCatalogClient.class);
        ProfileSkillClient profileSkills = mock(ProfileSkillClient.class);

        Job job = new Job();
        job.setCompanyId(UUID.randomUUID());
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(jobSkillRepository.findByJobId(any())).thenReturn(List.of(new JobSkill(jobId, JAVA, true, 60)));
        when(profileSkills.verifiedScores(any(), any())).thenReturn(Map.of(JAVA, new BigDecimal("90")));
        when(skillCatalog.skills(any())).thenReturn(List.of(new CatalogSkill(JAVA, "JAVA", "Java")));
        when(matchScoreRepository.findById(any())).thenReturn(Optional.empty());
        when(matchScoreRepository.save(any(MatchScore.class))).thenThrow(new DataIntegrityViolationException("dup"));

        MatchServiceImpl service = new MatchServiceImpl(jobRepository, jobSkillRepository, matchScoreRepository,
                mock(CompanyService.class), skillCatalog, profileSkills, mock(ApplicationClient.class),
                new ObjectMapper(), Clock.systemUTC());

        MatchResponse response = service.forViewer(jobId, userId,
                new AuthPrincipal(UUID.randomUUID(), Role.STAFF, null), "Bearer x");

        assertThat(response.score()).isEqualTo(100);
        assertThat(response.met()).hasSize(1);
        assertThat(response.met().get(0).skill()).isEqualTo("Java");
    }
}
