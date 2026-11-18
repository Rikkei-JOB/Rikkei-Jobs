package com.rikkei.jobs.company.util;

import com.rikkei.jobs.company.dto.request.JobRequest;
import com.rikkei.jobs.company.dto.request.JobSkillRequest;
import com.rikkei.jobs.company.entity.Job;
import com.rikkei.jobs.company.entity.JobSkill;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Quy tắc nghiệp vụ của JD (lưu nháp và gửi duyệt); hàm thuần, không phụ thuộc Spring. */
public final class JobRules {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private static final String MSG_DEADLINE = "Hạn nộp không được ở quá khứ";
    private static final String MSG_SALARY = "Lương tối đa phải lớn hơn hoặc bằng lương tối thiểu";
    private static final String MSG_REQUIRED = "Không được để trống";

    private JobRules() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }

    public static Map<String, String> saveErrors(JobRequest req, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (salaryInverted(req.getSalaryMin(), req.getSalaryMax())) {
            errors.put("salaryMax", MSG_SALARY);
        }
        if (req.getDeadline() != null && req.getDeadline().isBefore(today)) {
            errors.put("deadline", MSG_DEADLINE);
        }
        if (req.getSkills() != null) {
            Set<UUID> seen = new HashSet<>();
            for (JobSkillRequest s : req.getSkills()) {
                if (s != null && s.getSkillId() != null && !seen.add(s.getSkillId())) {
                    errors.put("skills", "Kỹ năng bị trùng");
                    break;
                }
            }
        }
        return errors;
    }

    public static Map<String, String> submitErrors(Job job, List<JobSkill> skills, LocalDate today) {
        Map<String, String> errors = new LinkedHashMap<>();
        requireText(errors, "title", job.getTitle());
        requireText(errors, "position", job.getPosition());
        requireNotNull(errors, "level", job.getLevel());
        requireNotNull(errors, "type", job.getType());
        requireNotNull(errors, "quantity", job.getQuantity());
        requireText(errors, "city", job.getCity());
        requireNotNull(errors, "workMode", job.getWorkMode());
        requireNotNull(errors, "deadline", job.getDeadline());
        requireText(errors, "description", job.getDescription());
        if (job.getDeadline() != null && job.getDeadline().isBefore(today)) {
            errors.put("deadline", MSG_DEADLINE);
        }
        if (salaryInverted(job.getSalaryMin(), job.getSalaryMax())) {
            errors.put("salaryMax", MSG_SALARY);
        }
        if (skills == null || skills.stream().noneMatch(JobSkill::isRequired)) {
            errors.put("skills", "Cần ít nhất một kỹ năng bắt buộc");
        }
        return errors;
    }

    /** Hết ngày hạn nộp theo giờ Việt Nam: 00:00 ngày kế tiếp. */
    public static OffsetDateTime applicationDeadline(LocalDate deadline) {
        return deadline.plusDays(1).atStartOfDay(ZONE).toOffsetDateTime();
    }

    private static boolean salaryInverted(Long min, Long max) {
        return min != null && max != null && min > max;
    }

    private static void requireText(Map<String, String> errors, String field, String value) {
        if (value == null || value.isBlank()) {
            errors.put(field, MSG_REQUIRED);
        }
    }

    private static void requireNotNull(Map<String, String> errors, String field, Object value) {
        if (value == null) {
            errors.put(field, MSG_REQUIRED);
        }
    }
}
