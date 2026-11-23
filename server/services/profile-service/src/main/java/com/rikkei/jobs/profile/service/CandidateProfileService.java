package com.rikkei.jobs.profile.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.profile.client.CandidateProfileSource;
import com.rikkei.jobs.profile.dto.CandidateProfileResponse;
import com.rikkei.jobs.profile.dto.SelfSkillRequest;
import com.rikkei.jobs.profile.dto.SkillProfileView;
import com.rikkei.jobs.profile.dto.StaffProfilePreview;
import com.rikkei.jobs.profile.dto.StaffReviewRow;
import com.rikkei.jobs.profile.dto.UpdateCandidateProfileRequest;
import com.rikkei.jobs.profile.entity.CandidateProfile;
import com.rikkei.jobs.profile.entity.ProfileReview.Action;
import com.rikkei.jobs.profile.entity.ProfileReview;
import com.rikkei.jobs.profile.entity.SkillProfileState;
import com.rikkei.jobs.profile.repository.CandidateProfileRepository;
import com.rikkei.jobs.profile.repository.ProfileReviewRepository;
import com.rikkei.jobs.profile.repository.SkillProfileStateRepository;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CandidateProfileService {
    private final CandidateProfileRepository profiles;
    private final SkillProfileStateRepository states;
    private final CandidateProfileSource source;
    private final Clock clock;
    private final ProfileReviewRepository reviews;
    private final SkillProfileService skills;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public CandidateProfileService(CandidateProfileRepository profiles, SkillProfileStateRepository states,
        CandidateProfileSource source, @Qualifier("skillProfileClock") Clock clock, ProfileReviewRepository reviews,
        SkillProfileService skills, JdbcTemplate jdbc, ObjectMapper json) {
        this.profiles = profiles; this.states = states; this.source = source; this.clock = clock;
        this.reviews = reviews;
        this.skills = skills;
        this.jdbc = jdbc;
        this.json = json;
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

    @Transactional(readOnly = true)
    public PageResponse<StaffReviewRow> reviewQueue(String status, String q, String classCode, String courseName,
                                                   int page, int size) {
        if (!List.of("PENDING", "PUBLIC", "REJECTED").contains(status)) {
            throw new BadRequestException("Trạng thái duyệt không hợp lệ.");
        }
        if (page < 1 || size < 1 || size > 100) {
            throw new BadRequestException("Trang phải từ 1, kích thước trang từ 1 đến 100.");
        }
        if ((long) (page - 1) * size > Integer.MAX_VALUE) {
            throw new BadRequestException("Vị trí trang vượt giới hạn truy vấn.");
        }
        var result = profiles.reviewQueue(status, q, classCode, courseName, PageRequest.of(page - 1, size));
        return PageResponse.of(result, result.getContent());
    }

    public String review(UUID owner, UUID actor, Action action, String reason) {
        if (action != Action.APPROVE && action != Action.REJECT) {
            throw new IllegalArgumentException("Unsupported staff review action");
        }
        var state = lockOwner(owner);
        var profile = pendingProfile(owner);
        boolean approved = action == Action.APPROVE;
        var now = clock.instant();
        profile.setVisibility(approved ? "PUBLIC" : "REJECTED");
        profile.setUpdatedAt(now);
        state.setApproved(approved);
        reviews.save(new ProfileReview(owner, action, reason, actor, now));
        return profile.getVisibility();
    }

    private CandidateProfile pendingProfile(UUID owner) {
        var profile = profiles.findById(owner)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ."));
        if (!"PENDING".equals(profile.getVisibility())) {
            throw new ConflictException("Chỉ hồ sơ đang chờ duyệt mới được xử lý.");
        }
        return profile;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public StaffProfilePreview preview(UUID owner) {
        var profile = pendingProfile(owner);
        return new StaffProfilePreview(owner, profile.getFullName(), profile.getHeadline(), profile.getSummary(),
            profile.getClassCode(), profile.getCourseName(), skills.getSkills(owner));
    }

    public void edit(UUID owner, UUID actor, String field, JsonNode value, String reason) {
        if (reason == null || reason.isBlank()) throw new BadRequestException("Cần cung cấp lý do chỉnh sửa.");
        lockOwner(owner);
        var profile = pendingProfile(owner);
        JsonNode before;
        JsonNode after;
        switch (field) {
            case "headline", "summary" -> {
                boolean headline = field.equals("headline");
                if (!value.isTextual() && !value.isNull())
                    throw new BadRequestException("Giá trị chỉnh sửa phải là văn bản.");
                String text = value.isNull() || value.textValue().isBlank() ? null : value.textValue().strip();
                if (text != null && text.length() > (headline ? 255 : 5000))
                    throw new BadRequestException("Nội dung chỉnh sửa vượt quá độ dài cho phép.");
                before = json.valueToTree(headline ? profile.getHeadline() : profile.getSummary());
                if (headline) profile.setHeadline(text);
                else profile.setSummary(text);
                after = json.valueToTree(text);
            }
            case "selfSkills" -> {
                var replacement = parseSelfSkills(value);
                before = selfSkillsValue(skills.getSkills(owner));
                after = selfSkillsValue(skills.replaceSelfSkills(owner, replacement));
            }
            default -> throw new BadRequestException("Trường này không được phép chỉnh sửa.");
        }
        var now = clock.instant();
        profile.setUpdatedAt(now);
        jdbc.update("""
            insert into profile.profile_edits(user_id, staff_id, field, old_value, new_value, reason, created_at)
            values (?, ?, ?, ?, ?, ?, ?)
            """, owner, actor, field, before.toString(), after.toString(), reason, now.atOffset(ZoneOffset.UTC));
    }

    private List<SelfSkillRequest> parseSelfSkills(JsonNode value) {
        if (!value.isArray()) throw new BadRequestException("Giá trị kỹ năng phải là một danh sách.");
        List<SelfSkillRequest> items = new ArrayList<>();
        for (var entry : value) {
            if (!entry.isObject() || entry.size() != 2 || !entry.path("skillId").isTextual()
                || !entry.path("selfScore").isNumber())
                throw new BadRequestException("Mỗi kỹ năng chỉ gồm skillId và selfScore.");
            try {
                var id = UUID.fromString(entry.get("skillId").textValue());
                items.add(new SelfSkillRequest(id, entry.get("selfScore").decimalValue()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Mã kỹ năng không hợp lệ.");
            }
        }
        return items;
    }

    private JsonNode selfSkillsValue(List<SkillProfileView> views) {
        return json.valueToTree(views.stream().filter(skill -> skill.selfScore() != null)
            .map(skill -> new SelfSkillRequest(skill.skill().id(), skill.selfScore())).toList());
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
