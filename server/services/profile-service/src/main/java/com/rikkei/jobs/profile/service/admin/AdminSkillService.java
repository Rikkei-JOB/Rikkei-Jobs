package com.rikkei.jobs.profile.service.admin;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.CreateSkillRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.DeleteSkillResult;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.SkillAdminView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.UpdateSkillRequest;
import com.rikkei.jobs.profile.entity.Skill;
import com.rikkei.jobs.profile.entity.admin.ChangeEntity;
import com.rikkei.jobs.profile.repository.SkillEvidenceRepository;
import com.rikkei.jobs.profile.repository.SkillMappingRepository;
import com.rikkei.jobs.profile.repository.SkillRepository;
import com.rikkei.jobs.profile.repository.SkillScoreRepository;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục kỹ năng (bảng của HS-01). Kỹ năng đã được dùng (bằng chứng, điểm hoặc ánh xạ nguồn) không xoá cứng,
 * chỉ tắt active để dữ liệu cũ còn nguyên. Mã kỹ năng không đổi sau khi tạo.
 */
@Service
@RequiredArgsConstructor
public class AdminSkillService {
    private final SkillRepository skills;
    private final SkillEvidenceRepository evidences;
    private final SkillScoreRepository scores;
    private final SkillMappingRepository mappings;
    private final ChangeLogService changeLog;

    @Transactional(readOnly = true)
    public List<SkillAdminView> list() {
        Set<UUID> used = usedSkillIds();
        return skills.findAll().stream()
                .sorted(Comparator.comparing(Skill::getGroup).thenComparing(Skill::getCode))
                .map(s -> view(s, used.contains(s.getId())))
                .toList();
    }

    @Transactional
    public SkillAdminView create(CreateSkillRequest request, UUID adminId) {
        String code = request.code().strip().toUpperCase(Locale.ROOT);
        if (skills.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Mã kỹ năng " + code + " đã tồn tại");
        }
        Skill skill = new Skill();
        skill.setCode(code);
        skill.setName(request.name().strip());
        skill.setGroup(request.group().strip());
        skill.setActive(true);
        Skill saved = skills.saveAndFlush(skill);
        changeLog.record(ChangeEntity.SKILL, saved.getId(), "code", null, saved.getCode(), request.reason(), adminId);
        changeLog.record(ChangeEntity.SKILL, saved.getId(), "name", null, saved.getName(), request.reason(), adminId);
        changeLog.record(ChangeEntity.SKILL, saved.getId(), "group", null, saved.getGroup(), request.reason(), adminId);
        return view(saved, false);
    }

    @Transactional
    public SkillAdminView update(UUID id, UpdateSkillRequest request, UUID adminId) {
        Skill skill = find(id);
        String name = request.name().strip();
        String group = request.group().strip();
        changeLog.record(ChangeEntity.SKILL, id, "name", skill.getName(), name, request.reason(), adminId);
        changeLog.record(ChangeEntity.SKILL, id, "group", skill.getGroup(), group, request.reason(), adminId);
        changeLog.record(ChangeEntity.SKILL, id, "active", skill.isActive(), request.active(), request.reason(), adminId);
        skill.setName(name);
        skill.setGroup(group);
        skill.setActive(request.active());
        return view(skills.save(skill), inUse(id));
    }

    /** Đã được dùng thì tắt active (bằng chứng và điểm cũ giữ nguyên); chưa dùng thì xoá hẳn. */
    @Transactional
    public DeleteSkillResult delete(UUID id, String reason, UUID adminId) {
        Skill skill = find(id);
        if (inUse(id)) {
            changeLog.record(ChangeEntity.SKILL, id, "active", skill.isActive(), false, reason, adminId);
            skill.setActive(false);
            skills.save(skill);
            return new DeleteSkillResult(id, false, true,
                    "Kỹ năng đã có bằng chứng hoặc ánh xạ nên chỉ được tắt, dữ liệu cũ giữ nguyên");
        }
        changeLog.record(ChangeEntity.SKILL, id, "deleted", skill.getCode() + " · " + skill.getName(), null,
                reason, adminId);
        skills.delete(skill);
        return new DeleteSkillResult(id, true, false, "Đã xoá kỹ năng");
    }

    private Skill find(UUID id) {
        return skills.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỹ năng"));
    }

    private boolean inUse(UUID id) {
        return evidences.existsBySkillId(id) || scores.existsByIdSkillId(id) || mappings.existsBySkillId(id);
    }

    private Set<UUID> usedSkillIds() {
        Set<UUID> used = new HashSet<>(evidences.findUsedSkillIds());
        used.addAll(scores.findUsedSkillIds());
        used.addAll(mappings.findUsedSkillIds());
        return used;
    }

    private static SkillAdminView view(Skill skill, boolean inUse) {
        return new SkillAdminView(skill.getId(), skill.getCode(), skill.getName(), skill.getGroup(),
                skill.isActive(), inUse);
    }
}
