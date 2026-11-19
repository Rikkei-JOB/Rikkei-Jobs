package com.rikkei.jobs.profile.service.admin;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.profile.dto.CandidateProfileResponse;
import com.rikkei.jobs.profile.dto.UpdateCandidateProfileRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.AdminProfileRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.EvidenceAdminView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.EvidenceUpdateRequest;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.OverrideRequest;
import com.rikkei.jobs.profile.entity.EvidenceSource;
import com.rikkei.jobs.profile.entity.ExternalSkillRecord;
import com.rikkei.jobs.profile.entity.Skill;
import com.rikkei.jobs.profile.entity.SkillEvidence;
import com.rikkei.jobs.profile.entity.admin.ChangeEntity;
import com.rikkei.jobs.profile.exception.FieldValidationException;
import com.rikkei.jobs.profile.repository.ExternalSkillRecordRepository;
import com.rikkei.jobs.profile.repository.SkillEvidenceRepository;
import com.rikkei.jobs.profile.repository.SkillRepository;
import com.rikkei.jobs.profile.service.CandidateProfileService;
import com.rikkei.jobs.profile.service.SkillProfileService;
import com.rikkei.jobs.profile.service.sync.SyncAdminService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ADMIN sửa hồ sơ ứng viên (HS-02) và bằng chứng kỹ năng (HS-01). Bắt buộc lý do; mỗi trường đổi ghi một dòng
 * change_logs với giá trị cũ và mới.
 *
 * <p>Bằng chứng sinh từ bản ghi nguồn ngoài (NT-05) được sửa ở bản ghi gốc qua cơ chế sửa tay của NT-05,
 * nên lần đồng bộ sau không ghi đè. Một bản ghi gốc có thể ánh xạ sang nhiều kỹ năng: sửa điểm hoặc tên ở đây
 * sẽ đổi mọi bằng chứng sinh từ bản ghi đó.
 */
@Service
@RequiredArgsConstructor
public class AdminRecordService {
    private final CandidateProfileService profiles;
    private final SkillEvidenceRepository evidences;
    private final SkillRepository skills;
    private final ExternalSkillRecordRepository externalRecords;
    private final SkillProfileService skillProfile;
    private final SyncAdminService syncAdmin;
    private final ChangeLogService changeLog;
    private final EntityManager entityManager;

    @Transactional
    public CandidateProfileResponse getProfile(UUID userId) {
        return profiles.get(userId);
    }

    @Transactional
    public CandidateProfileResponse updateProfile(UUID userId, AdminProfileRequest request, UUID adminId) {
        CandidateProfileResponse before = profiles.get(userId);
        UpdateCandidateProfileRequest update = new UpdateCandidateProfileRequest(request.fullName(), request.email(),
                request.dob(), request.gender(), request.phone(), request.address(), request.headline(),
                request.summary());
        CandidateProfileResponse after = profiles.update(userId, update);
        String reason = request.reason();
        log(userId, "fullName", before.fullName(), after.fullName(), reason, adminId);
        log(userId, "email", before.email(), after.email(), reason, adminId);
        log(userId, "dob", before.dob(), after.dob(), reason, adminId);
        log(userId, "gender", before.gender(), after.gender(), reason, adminId);
        log(userId, "phone", before.phone(), after.phone(), reason, adminId);
        log(userId, "address", before.address(), after.address(), reason, adminId);
        log(userId, "headline", before.headline(), after.headline(), reason, adminId);
        log(userId, "summary", before.summary(), after.summary(), reason, adminId);
        return after;
    }

    @Transactional(readOnly = true)
    public List<EvidenceAdminView> listEvidences(UUID userId) {
        List<SkillEvidence> rows = evidences.findAllByUserId(userId);
        Map<UUID, Skill> skillById = skills.findAllById(rows.stream().map(SkillEvidence::getSkillId).toList())
                .stream().collect(Collectors.toMap(Skill::getId, Function.identity()));
        return rows.stream()
                .sorted(Comparator.comparing(SkillEvidence::getRecordedAt).reversed()
                        .thenComparing(SkillEvidence::getSourceRef))
                .map(e -> view(e, skillById.get(e.getSkillId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public EvidenceAdminView getEvidence(UUID id) {
        SkillEvidence evidence = findEvidence(id);
        return view(evidence, skills.findById(evidence.getSkillId()).orElse(null));
    }

    @Transactional
    public EvidenceAdminView updateEvidence(UUID id, EvidenceUpdateRequest request, UUID adminId) {
        String title = request.title() == null || request.title().isBlank() ? null : request.title().strip();
        BigDecimal score = request.score();
        if (title == null && score == null) {
            throw new FieldValidationException("score", "Nhập điểm hoặc tên mới cần sửa");
        }
        SkillEvidence evidence = findEvidence(id);
        if (evidence.getSource() == EvidenceSource.SELF) {
            throw new BadRequestException("Không sửa kỹ năng tự khai của ứng viên");
        }
        UUID userId = evidence.getUserId();
        skillProfile.lockForUpdate(userId);
        entityManager.refresh(evidence);
        String oldTitle = evidence.getTitle();
        BigDecimal oldScore = evidence.getScore();
        String reason = request.reason().strip();

        if (evidence.getExternalRecordId() != null) {
            ExternalSkillRecord record = externalRecords.findById(evidence.getExternalRecordId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi nguồn của bằng chứng"));
            if (title != null && !title.equals(oldTitle)) {
                syncAdmin.override(record.getId(), new OverrideRequest("title", title, reason), adminId);
            }
            if (score != null && score.compareTo(oldScore) != 0) {
                // Bằng chứng theo thang 0–100, bản ghi gốc theo thang của nguồn (ví dụ 10): đổi thang, làm tròn 2 số lẻ.
                BigDecimal raw = score.multiply(record.getScoreMax())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                syncAdmin.override(record.getId(), new OverrideRequest("score", raw.toPlainString(), reason), adminId);
            }
            entityManager.flush();
            entityManager.refresh(evidence);
        } else {
            if (title != null) {
                evidence.setTitle(title);
            }
            if (score != null) {
                evidence.setScore(score);
            }
            evidences.saveAndFlush(evidence);
            skillProfile.recomputeScores(userId);
        }

        log(ChangeEntity.EVIDENCE, id, "title", oldTitle, evidence.getTitle(), reason, adminId);
        log(ChangeEntity.EVIDENCE, id, "score", oldScore, evidence.getScore(), reason, adminId);
        return view(evidence, skills.findById(evidence.getSkillId()).orElse(null));
    }

    private SkillEvidence findEvidence(UUID id) {
        return evidences.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bằng chứng"));
    }

    private void log(UUID userId, String field, Object before, Object after, String reason, UUID adminId) {
        log(ChangeEntity.PROFILE, userId, field, before, after, reason, adminId);
    }

    private void log(ChangeEntity entity, UUID id, String field, Object before, Object after, String reason,
                     UUID adminId) {
        if (before instanceof BigDecimal a && after instanceof BigDecimal b && a.compareTo(b) == 0) {
            return;
        }
        if (!Objects.equals(before, after)) {
            changeLog.record(entity, id, field, before, after, reason, adminId);
        }
    }

    static EvidenceAdminView view(SkillEvidence e, Skill skill) {
        return new EvidenceAdminView(e.getId(), e.getUserId(), e.getSkillId(), skill == null ? null : skill.getName(),
                e.getSource(), e.getTitle(), e.getScore(), e.getRecordedAt(), e.isVerified(),
                e.getExternalRecordId() != null);
    }
}
