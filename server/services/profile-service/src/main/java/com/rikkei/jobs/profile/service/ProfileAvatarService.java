package com.rikkei.jobs.profile.service;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.profile.dto.CandidateProfileResponse;
import com.rikkei.jobs.profile.repository.CandidateProfileRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileAvatarService {
    private final CandidateProfileService profiles;
    private final CandidateProfileRepository repository;
    private final ProfileFileService files;
    private final Clock clock;
    public ProfileAvatarService(CandidateProfileService profiles, CandidateProfileRepository repository,
        ProfileFileService files, @Qualifier("skillProfileClock") Clock clock) {
        this.profiles = profiles; this.repository = repository; this.files = files; this.clock = clock;
    }
    @Transactional
    public CandidateProfileResponse update(UUID owner, String url) {
        if (url != null) {
            var file = files.requireOwned(owner, url);
            if (!file.getContentType().startsWith("image/")) throw new BadRequestException("Avatar chỉ nhận JPG hoặc PNG.");
        }
        profiles.get(owner);
        var profile = repository.findById(owner).orElseThrow();
        profile.setAvatarUrl(url); profile.setUpdatedAt(clock.instant());
        return CandidateProfileResponse.from(repository.save(profile));
    }
}
