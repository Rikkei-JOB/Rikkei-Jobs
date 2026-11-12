package com.rikkei.jobs.recruitment.service;
import com.rikkei.jobs.recruitment.entity.*;
import java.util.UUID;
/** Internal API: HR callers must validate role and company ownership before transition(). */
public interface ApplicationTransitionService {
    Application transition(UUID id, ApplicationStatus target, UUID actorId, String note);
    Application withdraw(UUID id, UUID userId, String reason);
}
