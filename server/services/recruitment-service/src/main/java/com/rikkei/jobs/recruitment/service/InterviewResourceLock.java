package com.rikkei.jobs.recruitment.service;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;

@Component @RequiredArgsConstructor
public class InterviewResourceLock {
    private final JdbcTemplate jdbc;
    public static String email(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    @Transactional(propagation=Propagation.MANDATORY)
    public void lock(UUID candidateId,String interviewerEmail) {
        var keys=new ArrayList<>(List.of("candidate:"+candidateId,"interviewer:"+email(interviewerEmail)));
        Collections.sort(keys);
        for(var key:keys) jdbc.query("select pg_advisory_xact_lock(hashtextextended(?, 6006))",rs->{},key);
    }
}
