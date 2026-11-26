package com.rikkei.jobs.profile.controller;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.Roles;
import com.rikkei.jobs.profile.dto.SkillProfileView.SkillInfo;
import com.rikkei.jobs.profile.entity.Skill;
import com.rikkei.jobs.profile.repository.SkillRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị danh mục kỹ năng (qua gateway: /api/v1/admin/skills/**). Đọc danh mục vẫn dùng GET /skills (công khai).
 * Mã kỹ năng không đổi được sau khi tạo vì JD, điểm kỹ năng và ánh xạ đồng bộ LMS tham chiếu theo id/mã.
 * Không xoá kỹ năng: dữ liệu cũ (JD, điểm) còn trỏ tới.
 */
@Tag(name = "Quản trị - Danh mục kỹ năng")
@RestController
@RequestMapping("/admin/skills")
@RequiredArgsConstructor
@PreAuthorize(Roles.ADMIN)
public class AdminSkillController {
    private final SkillRepository skills;

    public record CreateSkillRequest(
        @NotBlank(message = "Vui lòng nhập mã kỹ năng.")
        @Pattern(regexp = "[A-Za-z0-9_]{2,100}", message = "Mã gồm 2–100 ký tự chữ, số hoặc gạch dưới.") String code,
        @NotBlank(message = "Vui lòng nhập tên kỹ năng.") @Size(max = 255, message = "Tên tối đa 255 ký tự.") String name,
        @NotBlank(message = "Vui lòng nhập nhóm kỹ năng.") @Size(max = 100, message = "Nhóm tối đa 100 ký tự.") String group) { }

    public record UpdateSkillRequest(
        @NotBlank(message = "Vui lòng nhập tên kỹ năng.") @Size(max = 255, message = "Tên tối đa 255 ký tự.") String name,
        @NotBlank(message = "Vui lòng nhập nhóm kỹ năng.") @Size(max = 100, message = "Nhóm tối đa 100 ký tự.") String group) { }

    @Operation(summary = "Thêm kỹ năng", description = "Vai trò: ADMIN. Mã được viết hoa; trùng mã trả 409.")
    @ApiResponse(responseCode = "409", description = "CONFLICT: mã kỹ năng đã tồn tại")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public SkillInfo create(@Valid @RequestBody CreateSkillRequest request) {
        String code = request.code().strip().toUpperCase(Locale.ROOT);
        if (skills.findByCode(code).isPresent()) throw new ConflictException("Mã kỹ năng " + code + " đã tồn tại.");
        Skill skill = new Skill();
        skill.setCode(code);
        skill.setName(request.name().strip());
        skill.setGroup(request.group().strip());
        return info(skills.save(skill));
    }

    @Operation(summary = "Sửa tên, nhóm kỹ năng", description = "Vai trò: ADMIN. Mã kỹ năng giữ nguyên.")
    @ApiResponse(responseCode = "404", description = "NOT_FOUND: không có kỹ năng")
    @PutMapping("/{id}")
    @Transactional
    public SkillInfo update(@PathVariable UUID id, @Valid @RequestBody UpdateSkillRequest request) {
        Skill skill = skills.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy kỹ năng."));
        skill.setName(request.name().strip());
        skill.setGroup(request.group().strip());
        return info(skills.save(skill));
    }

    private static SkillInfo info(Skill s) {
        return new SkillInfo(s.getId(), s.getCode(), s.getName(), s.getGroup());
    }
}
