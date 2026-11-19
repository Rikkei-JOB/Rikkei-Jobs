package com.rikkei.jobs.profile.service.admin;

import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.CriterionRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.CriterionView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.TemplateRequest;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.TemplateView;
import com.rikkei.jobs.profile.entity.admin.ChangeEntity;
import com.rikkei.jobs.profile.entity.admin.EvaluationCriterion;
import com.rikkei.jobs.profile.entity.admin.EvaluationTemplate;
import com.rikkei.jobs.profile.repository.admin.EvaluationTemplateRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mẫu phiếu đánh giá (bảng của DG-01) kèm tiêu chí. Sửa mẫu thay toàn bộ danh sách tiêu chí;
 * phiếu đã chấm của DG-01 phải giữ bản chụp tiêu chí riêng nên không bị ảnh hưởng.
 */
@Service
public class EvaluationTemplateService {
    private final EvaluationTemplateRepository templates;
    private final ChangeLogService changeLog;
    private final Clock clock;

    public EvaluationTemplateService(EvaluationTemplateRepository templates, ChangeLogService changeLog,
                                     @Qualifier("skillProfileClock") Clock clock) {
        this.templates = templates;
        this.changeLog = changeLog;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TemplateView> list(boolean activeOnly) {
        return templates.findAllWithCriteria().stream()
                .filter(t -> !activeOnly || t.isActive())
                .map(EvaluationTemplateService::view)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateView get(UUID id) {
        return view(find(id));
    }

    @Transactional
    public TemplateView create(TemplateRequest request, UUID adminId) {
        String name = request.name().strip();
        if (templates.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Đã có mẫu phiếu tên \"" + name + "\"");
        }
        Instant now = now();
        EvaluationTemplate template = new EvaluationTemplate();
        template.setId(UUID.randomUUID());
        template.setName(name);
        template.setDescription(blankToNull(request.description()));
        template.setActive(request.active() == null || request.active());
        template.setCreatedAt(now);
        template.setUpdatedAt(now);
        replaceCriteria(template, request.criteria());
        EvaluationTemplate saved = templates.save(template);
        changeLog.record(ChangeEntity.EVALUATION_TEMPLATE, saved.getId(), "name", null, saved.getName(),
                request.reason(), adminId);
        changeLog.record(ChangeEntity.EVALUATION_TEMPLATE, saved.getId(), "criteria", null,
                summary(saved.getCriteria()), request.reason(), adminId);
        return view(saved);
    }

    @Transactional
    public TemplateView update(UUID id, TemplateRequest request, UUID adminId) {
        EvaluationTemplate template = find(id);
        String name = request.name().strip();
        if (templates.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Đã có mẫu phiếu tên \"" + name + "\"");
        }
        String description = blankToNull(request.description());
        boolean active = request.active() == null ? template.isActive() : request.active();
        String oldCriteria = summary(template.getCriteria());

        changeLog.record(ChangeEntity.EVALUATION_TEMPLATE, id, "name", template.getName(), name, request.reason(), adminId);
        changeLog.record(ChangeEntity.EVALUATION_TEMPLATE, id, "description", template.getDescription(), description,
                request.reason(), adminId);
        changeLog.record(ChangeEntity.EVALUATION_TEMPLATE, id, "active", template.isActive(), active,
                request.reason(), adminId);

        template.setName(name);
        template.setDescription(description);
        template.setActive(active);
        replaceCriteria(template, request.criteria());
        template.setUpdatedAt(now());
        EvaluationTemplate saved = templates.save(template);
        changeLog.record(ChangeEntity.EVALUATION_TEMPLATE, id, "criteria", oldCriteria, summary(saved.getCriteria()),
                request.reason(), adminId);
        return view(saved);
    }

    @Transactional
    public void delete(UUID id, String reason, UUID adminId) {
        EvaluationTemplate template = find(id);
        changeLog.record(ChangeEntity.EVALUATION_TEMPLATE, id, "deleted",
                template.getName() + " | " + summary(template.getCriteria()), null, reason, adminId);
        templates.delete(template);
    }

    private EvaluationTemplate find(UUID id) {
        return templates.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mẫu phiếu"));
    }

    /** Thay toàn bộ tiêu chí; thứ tự theo vị trí trong request. */
    private static void replaceCriteria(EvaluationTemplate template, List<CriterionRequest> requested) {
        template.getCriteria().clear();
        int order = 1;
        for (CriterionRequest item : requested) {
            EvaluationCriterion criterion = new EvaluationCriterion();
            criterion.setId(UUID.randomUUID());
            criterion.setTemplate(template);
            criterion.setName(item.name().strip());
            criterion.setDescription(blankToNull(item.description()));
            criterion.setWeight(item.weight());
            criterion.setMaxScore(item.maxScore());
            criterion.setSortOrder(order++);
            template.getCriteria().add(criterion);
        }
    }

    private static String summary(List<EvaluationCriterion> criteria) {
        return criteria.stream()
                .map(c -> c.getName() + " (" + plain(c.getWeight()) + "%, /" + c.getMaxScore() + ")")
                .collect(Collectors.joining("; "));
    }

    private static TemplateView view(EvaluationTemplate template) {
        return new TemplateView(template.getId(), template.getName(), template.getDescription(), template.isActive(),
                template.getCriteria().stream().map(c -> new CriterionView(c.getId(), c.getName(), c.getDescription(),
                        c.getWeight(), c.getMaxScore(), c.getSortOrder())).toList(),
                template.getUpdatedAt());
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static String plain(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }
}
