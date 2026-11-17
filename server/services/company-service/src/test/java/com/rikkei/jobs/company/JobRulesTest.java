package com.rikkei.jobs.company;

import static org.assertj.core.api.Assertions.assertThat;

import com.rikkei.jobs.company.dto.request.JobRequest;
import com.rikkei.jobs.company.dto.request.JobSkillRequest;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobLevel;
import com.rikkei.jobs.company.entity.JobSkill;
import com.rikkei.jobs.company.entity.JobType;
import com.rikkei.jobs.company.entity.WorkMode;
import com.rikkei.jobs.company.util.JobRules;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JobRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    private static JobSkillRequest skillReq(UUID id, boolean required) {
        JobSkillRequest s = new JobSkillRequest();
        s.setSkillId(id);
        s.setRequired(required);
        s.setMinScore(50);
        return s;
    }

    private static Job fullJob() {
        Job j = new Job();
        j.setTitle("Backend Java");
        j.setPosition("Backend Developer");
        j.setLevel(JobLevel.JUNIOR);
        j.setType(JobType.FULLTIME);
        j.setQuantity(2);
        j.setCity("Hà Nội");
        j.setWorkMode(WorkMode.HYBRID);
        j.setDeadline(TODAY.plusDays(10));
        j.setDescription("Mô tả");
        return j;
    }

    @Test
    void todayUsesVietnamTime() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-17T23:30:00Z"), ZoneOffset.UTC);
        assertThat(JobRules.today(clock)).isEqualTo(LocalDate.of(2026, 11, 18));

        JobRequest req = new JobRequest();
        req.setDeadline(LocalDate.of(2026, 11, 17));
        assertThat(JobRules.saveErrors(req, JobRules.today(clock))).containsKey("deadline");
        req.setDeadline(LocalDate.of(2026, 11, 18));
        assertThat(JobRules.saveErrors(req, JobRules.today(clock))).doesNotContainKey("deadline");
    }

    @Test
    void salaryMinGreaterThanMaxIsSalaryMaxError() {
        JobRequest req = new JobRequest();
        req.setSalaryMin(20L);
        req.setSalaryMax(10L);
        assertThat(JobRules.saveErrors(req, TODAY)).containsOnlyKeys("salaryMax");
    }

    @Test
    void duplicateSkillIsSkillsError() {
        UUID id = UUID.randomUUID();
        JobRequest req = new JobRequest();
        req.setSkills(List.of(skillReq(id, true), skillReq(id, false)));
        assertThat(JobRules.saveErrors(req, TODAY)).containsOnlyKeys("skills");
    }

    @Test
    void validRequestHasNoErrors() {
        JobRequest req = new JobRequest();
        req.setTitle("JD");
        req.setSalaryMin(10L);
        req.setSalaryMax(10L);
        req.setDeadline(TODAY);
        req.setSkills(List.of(skillReq(UUID.randomUUID(), true), skillReq(UUID.randomUUID(), false)));
        assertThat(JobRules.saveErrors(req, TODAY)).isEmpty();
    }

    @Test
    void submitListsEveryMissingField() {
        Job j = new Job();
        j.setTitle("Chỉ có tiêu đề");
        assertThat(JobRules.submitErrors(j, List.of(), TODAY).keySet())
                .isEqualTo(Set.of("position", "level", "type", "quantity", "city", "workMode",
                        "deadline", "description", "skills"));
    }

    @Test
    void submitNeedsRequiredSkill() {
        Job j = fullJob();
        UUID id = j.getId();
        JobSkill optional = new JobSkill(id, UUID.randomUUID(), false, 50);
        assertThat(JobRules.submitErrors(j, List.of(optional), TODAY)).containsOnlyKeys("skills");
        JobSkill mandatory = new JobSkill(id, UUID.randomUUID(), true, 50);
        assertThat(JobRules.submitErrors(j, List.of(optional, mandatory), TODAY)).isEmpty();
    }

    @Test
    void applicationDeadlineIsNextMidnightInVietnam() {
        assertThat(JobRules.applicationDeadline(LocalDate.of(2026, 12, 31)))
                .isEqualTo(OffsetDateTime.parse("2027-01-01T00:00:00+07:00"));
    }
}
