package com.rikkei.jobs.recruitment;

import com.rikkei.jobs.recruitment.client.JobMatchClient;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.rikkei.jobs.recruitment.dto.response.*;
import com.rikkei.jobs.recruitment.service.MatchLookupService;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class MatchLookupServiceTest {
    final JobMatchClient client=mock(JobMatchClient.class);
    final UUID job=UUID.randomUUID(),user=UUID.randomUUID();
    @Test void zeroAndEmptySkillListsAreAvailable() {
        var score=new MatchResponse(BigDecimal.ZERO,false,List.of(),List.of());
        when(client.get(job,user,"Bearer hr")).thenReturn(Optional.of(score));
        var result=new MatchLookupService(client).lookup(job,user,"Bearer hr");
        assertEquals(MatchStatus.AVAILABLE,result.matchStatus());assertEquals(BigDecimal.ZERO,result.match().score());
    }
    @Test void missingOrDependencyFailureIsUnavailable() {
        var service=new MatchLookupService(client);
        when(client.get(any(),any(),any())).thenReturn(Optional.empty());
        assertEquals(new MatchLookupResponse(MatchStatus.UNAVAILABLE,null),service.lookup(job,user,"Bearer hr"));
        when(client.get(any(),any(),any())).thenThrow(new RecruitmentDependencyException(502,"MATCH_CONTRACT_ERROR","Bad upstream"));
        assertEquals(new MatchLookupResponse(MatchStatus.UNAVAILABLE,null),service.lookup(job,user,"Bearer hr"));
    }
    @Test void unrelatedProgrammingErrorsAreNotHidden() {
        when(client.get(any(),any(),any())).thenThrow(new IllegalStateException("Programming error"));
        assertThrows(IllegalStateException.class,()->new MatchLookupService(client).lookup(job,user,"Bearer hr"));
    }
}
