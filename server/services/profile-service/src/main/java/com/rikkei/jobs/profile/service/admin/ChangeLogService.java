package com.rikkei.jobs.profile.service.admin;

import com.rikkei.jobs.profile.config.SyncProperties;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.ChangeLogView;
import com.rikkei.jobs.profile.dto.sync.SyncDtos.PageView;
import com.rikkei.jobs.profile.entity.admin.ChangeEntity;
import com.rikkei.jobs.profile.entity.admin.ChangeLog;
import com.rikkei.jobs.profile.exception.FieldValidationException;
import com.rikkei.jobs.profile.repository.admin.ChangeLogRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Ghi và tra change_logs. Ghi trong cùng transaction với thay đổi: thay đổi rollback thì nhật ký cũng không còn. */
@Service
public class ChangeLogService {
    private static final int MAX_VALUE_LENGTH = 5000;

    private final ChangeLogRepository logs;
    private final Clock clock;
    private final ZoneId zone;

    public ChangeLogService(ChangeLogRepository logs, @Qualifier("skillProfileClock") Clock clock,
                            SyncProperties syncProperties) {
        this.logs = logs;
        this.clock = clock;
        this.zone = ZoneId.of(syncProperties.getZone());
    }

    /** Ghi một dòng nếu giá trị thực sự đổi; trả true khi có ghi. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean record(ChangeEntity entity, Object entityId, String field, Object oldValue, Object newValue,
                          String reason, UUID adminId) {
        String before = text(oldValue);
        String after = text(newValue);
        if (Objects.equals(before, after)) {
            return false;
        }
        ChangeLog log = new ChangeLog();
        log.setId(UUID.randomUUID());
        log.setEntity(entity);
        log.setEntityId(String.valueOf(entityId));
        log.setField(field);
        log.setOldValue(before);
        log.setNewValue(after);
        log.setReason(reason == null || reason.isBlank() ? null : reason.strip());
        log.setAdminId(adminId);
        log.setCreatedAt(clock.instant().truncatedTo(ChronoUnit.MICROS));
        logs.save(log);
        return true;
    }

    /** from/to là ngày theo múi giờ của hệ thống (mặc định Asia/Ho_Chi_Minh), tính cả ngày to. */
    @Transactional(readOnly = true)
    public PageView<ChangeLogView> search(ChangeEntity entity, String entityId, LocalDate from, LocalDate to,
                                          int page, int size) {
        if (page < 1) {
            throw new FieldValidationException("page", "page phải từ 1");
        }
        if (size < 1 || size > 100) {
            throw new FieldValidationException("size", "size phải từ 1 đến 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new FieldValidationException("from", "Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        Instant start = from == null ? null : from.atStartOfDay(zone).toInstant();
        Instant end = to == null ? null : to.plusDays(1).atStartOfDay(zone).toInstant();
        String key = entityId == null || entityId.isBlank() ? null : entityId.strip();
        Page<ChangeLog> result = logs.search(entity, key, start, end, PageRequest.of(page - 1, size,
                org.springframework.data.domain.Sort.by("createdAt").descending().and(
                        org.springframework.data.domain.Sort.by("id"))));
        return new PageView<>(result.getContent().stream().map(ChangeLogService::view).toList(),
                result.getTotalElements(), page, size);
    }

    private static ChangeLogView view(ChangeLog log) {
        return new ChangeLogView(log.getId(), log.getEntity(), log.getEntityId(), log.getField(), log.getOldValue(),
                log.getNewValue(), log.getReason(), log.getAdminId(), log.getCreatedAt());
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = value instanceof BigDecimal number ? plain(number) : value.toString();
        return text.length() > MAX_VALUE_LENGTH ? text.substring(0, MAX_VALUE_LENGTH) : text;
    }

    private static String plain(BigDecimal number) {
        BigDecimal stripped = number.stripTrailingZeros();
        return (stripped.scale() < 0 ? stripped.setScale(0) : stripped).toPlainString();
    }
}
