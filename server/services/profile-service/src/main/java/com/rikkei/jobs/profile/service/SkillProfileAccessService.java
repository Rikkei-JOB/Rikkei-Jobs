package com.rikkei.jobs.profile.service;

import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.profile.dto.SkillProfileView;
import com.rikkei.jobs.profile.repository.SkillProfileStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class SkillProfileAccessService {
    private final SkillProfileStateRepository states;
    private final SkillProfileService core;

    public List<SkillProfileView> getSkills(UUID ownerId, UUID viewerId) {
        if (viewerId == null || ownerId == null || (!ownerId.equals(viewerId)
                && !states.findById(ownerId).map(s -> s.isConsented() && s.isApproved()).orElse(false))) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ năng lực");
        }
        return core.getSkills(ownerId);
    }
}
