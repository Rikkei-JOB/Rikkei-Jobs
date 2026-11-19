package com.rikkei.jobs.recruitment.service;

import com.rikkei.jobs.recruitment.client.JobMatchClient;
import com.rikkei.jobs.recruitment.config.RecruitmentDependencyException;
import com.rikkei.jobs.recruitment.dto.response.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Advisory dependency boundary. Call only after application access has been checked. */
@Service @RequiredArgsConstructor @Slf4j
public class MatchLookupService {
    private final JobMatchClient matches;

    public MatchLookupResponse lookup(UUID jobId, UUID candidateId, String bearer) {
        try {
            return matches.get(jobId,candidateId,bearer)
                .map(value -> new MatchLookupResponse(MatchStatus.AVAILABLE,value))
                .orElseGet(() -> new MatchLookupResponse(MatchStatus.UNAVAILABLE,null));
        } catch (RecruitmentDependencyException ex) {
            log.warn("Advisory match unavailable code={} jobId={} candidateId={}",ex.getCode(),jobId,candidateId);
            return new MatchLookupResponse(MatchStatus.UNAVAILABLE,null);
        }
    }
}
