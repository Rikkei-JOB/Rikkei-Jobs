package com.rikkei.jobs.profile.service;

import com.rikkei.jobs.profile.client.CandidateProfileSource;
import com.rikkei.jobs.profile.dto.CandidateProfileResponse;
import com.rikkei.jobs.profile.dto.UpdateCandidateProfileRequest;
import com.rikkei.jobs.profile.entity.CandidateProfile;
import com.rikkei.jobs.profile.repository.CandidateProfileRepository;
import com.rikkei.jobs.profile.repository.SkillProfileStateRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CandidateProfileService {
    private final CandidateProfileRepository profiles;
    private final SkillProfileStateRepository states;
    private final CandidateProfileSource source;
    private final Clock clock;
    public CandidateProfileService(CandidateProfileRepository profiles, SkillProfileStateRepository states,
        CandidateProfileSource source, @Qualifier("skillProfileClock") Clock clock) {
        this.profiles = profiles; this.states = states; this.source = source; this.clock = clock;
    }
    public CandidateProfileResponse get(UUID owner) {
        return CandidateProfileResponse.from(getOrCreate(owner));
    }
    public CandidateProfileResponse update(UUID owner, UpdateCandidateProfileRequest request) {
        CandidateProfile p = getOrCreate(owner);
        p.setFullName(request.fullName()); p.setEmail(request.email()); p.setDob(request.dob());
        p.setGender(request.gender()); p.setPhone(request.phone()); p.setAddress(request.address());
        p.setHeadline(request.headline()); p.setSummary(request.summary()); p.setUpdatedAt(clock.instant());
        return CandidateProfileResponse.from(profiles.save(p));
    }
    private CandidateProfile getOrCreate(UUID owner) {
        var existing = profiles.findById(owner);
        if (existing.isPresent()) return existing.get();
        // Reuse HS-01's per-user lock so concurrent first reads cannot overwrite edits.
        states.ensureExists(owner);
        states.lockByUserId(owner).orElseThrow();
        return profiles.findById(owner).orElseGet(() -> {
            var initial = source.fetch(owner);
            var p = new CandidateProfile();
            p.setUserId(owner); p.setFullName(initial.fullName()); p.setEmail(initial.email());
            p.setProgram(initial.program()); p.setClassCode(initial.classCode());
            p.setCourseName(initial.courseName()); p.setUpdatedAt(clock.instant());
            return profiles.save(p);
        });
    }
}
