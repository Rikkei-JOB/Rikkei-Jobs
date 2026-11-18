package com.rikkei.jobs.profile.service;

import com.rikkei.jobs.profile.client.CandidateProfileSource;
import com.rikkei.jobs.profile.dto.CandidateProfileResponse;
import com.rikkei.jobs.profile.dto.UpdateCandidateProfileRequest;
import com.rikkei.jobs.profile.entity.CandidateProfile;
import com.rikkei.jobs.profile.entity.ProfileReview;
import com.rikkei.jobs.profile.entity.ProfileReview.Action;
import com.rikkei.jobs.profile.entity.SkillProfileState;
import com.rikkei.jobs.profile.repository.ProfileReviewRepository;
import com.rikkei.jobs.common.config.exception.ConflictException;
import java.util.List;
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
    private final ProfileReviewRepository reviews;
    public CandidateProfileService(CandidateProfileRepository profiles, SkillProfileStateRepository states,
        CandidateProfileSource source, @Qualifier("skillProfileClock") Clock clock, ProfileReviewRepository reviews) {
        this.profiles = profiles; this.states = states; this.source = source; this.clock = clock;
        this.reviews = reviews;
    }
    public CandidateProfileResponse get(UUID owner) {
        return CandidateProfileResponse.from(profiles.findById(owner).orElseGet(() -> forUpdate(owner)));
    }
    public CandidateProfileResponse update(UUID owner, UpdateCandidateProfileRequest request) {
        CandidateProfile p = forUpdate(owner);
        p.setFullName(request.fullName()); p.setEmail(request.email()); p.setDob(request.dob());
        p.setGender(request.gender()); p.setPhone(request.phone()); p.setAddress(request.address());
        p.setHeadline(request.headline()); p.setSummary(request.summary()); p.setUpdatedAt(clock.instant());
        return CandidateProfileResponse.from(profiles.save(p));
    }
    private SkillProfileState lockOwner(UUID owner) {
        states.ensureExists(owner);
        return states.lockByUserId(owner).orElseThrow();
    }

    CandidateProfile forUpdate(UUID owner) {
        lockOwner(owner);
        return findOrCreate(owner);
    }

    @Transactional(readOnly = true)
    public List<ProfileReview> history(UUID owner) {
        return reviews.findByUserIdOrderByIdDesc(owner);
    }

    public CandidateProfileResponse publish(UUID owner) {
        var state = lockOwner(owner);
        var profile = findOrCreate(owner);
        if (!List.of("PRIVATE", "REJECTED").contains(profile.getVisibility())) {
            throw new ConflictException("Chỉ hồ sơ riêng tư hoặc bị trả lại mới được gửi duyệt.");
        }
        var now = clock.instant();
        profile.setVisibility("PENDING");
        profile.setConsentAt(now);
        profile.setConsentWithdrawnAt(null);
        profile.setUpdatedAt(now);
        state.setConsented(true);
        state.setApproved(false);
        reviews.save(new ProfileReview(owner, Action.SUBMIT, null, owner, now));
        return CandidateProfileResponse.from(profile);
    }

    public CandidateProfileResponse withdraw(UUID owner) {
        var state = lockOwner(owner);
        var profile = profiles.findById(owner)
            .filter(p -> List.of("PENDING", "PUBLIC").contains(p.getVisibility()))
            .orElseThrow(() -> new ConflictException("Chỉ hồ sơ chờ duyệt hoặc đã công khai mới có thể rút."));
        var now = clock.instant();
        profile.setVisibility("PRIVATE");
        profile.setConsentWithdrawnAt(now);
        profile.setUpdatedAt(now);
        state.setConsented(false);
        state.setApproved(false);
        reviews.save(new ProfileReview(owner, Action.WITHDRAW, null, owner, now));
        return CandidateProfileResponse.from(profile);
    }

    private CandidateProfile findOrCreate(UUID owner) {
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
