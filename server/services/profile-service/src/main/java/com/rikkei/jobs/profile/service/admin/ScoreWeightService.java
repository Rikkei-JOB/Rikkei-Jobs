package com.rikkei.jobs.profile.service.admin;

import com.rikkei.jobs.profile.dto.admin.AdminDtos.ScoreWeightItem;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.ScoreWeightsView;
import com.rikkei.jobs.profile.dto.admin.AdminDtos.UpdateScoreWeightsRequest;
import com.rikkei.jobs.profile.entity.admin.ChangeEntity;
import com.rikkei.jobs.profile.entity.admin.ScoreWeight;
import com.rikkei.jobs.profile.exception.FieldValidationException;
import com.rikkei.jobs.profile.repository.admin.ScoreWeightRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Trọng số nguồn điểm (DG-03 dùng chung). Phải gửi đủ 4 nguồn, mỗi nguồn một lần, tổng đúng bằng 100. */
@Service
public class ScoreWeightService {
    /** Thứ tự hiển thị cố định. */
    public static final List<String> SOURCES = List.of("LMS", "EXAM", "INTERNSHIP", "MENTOR");
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ScoreWeightRepository weights;
    private final ChangeLogService changeLog;
    private final Clock clock;

    public ScoreWeightService(ScoreWeightRepository weights, ChangeLogService changeLog,
                              @Qualifier("skillProfileClock") Clock clock) {
        this.weights = weights;
        this.changeLog = changeLog;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ScoreWeightsView get() {
        return view(weights.findAll());
    }

    @Transactional
    public ScoreWeightsView update(UpdateScoreWeightsRequest request, UUID adminId) {
        Map<String, BigDecimal> requested = new HashMap<>();
        for (ScoreWeightItem item : request.weights()) {
            String source = item.source().strip().toUpperCase(Locale.ROOT);
            if (!SOURCES.contains(source)) {
                throw new FieldValidationException("weights", "Nguồn không hợp lệ: " + item.source());
            }
            if (item.weight().stripTrailingZeros().scale() > 2) {
                throw new FieldValidationException("weights", "Trọng số có tối đa 2 chữ số thập phân");
            }
            if (requested.put(source, item.weight()) != null) {
                throw new FieldValidationException("weights", "Nguồn " + source + " bị lặp");
            }
        }
        if (!requested.keySet().containsAll(SOURCES)) {
            throw new FieldValidationException("weights", "Phải nhập đủ trọng số cho " + String.join(", ", SOURCES));
        }
        BigDecimal total = requested.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(HUNDRED) != 0) {
            throw new FieldValidationException("weights",
                    "Tổng trọng số phải bằng 100 (hiện tại: " + total.stripTrailingZeros().toPlainString() + ")");
        }

        Map<String, ScoreWeight> current = weights.findAll().stream()
                .collect(Collectors.toMap(ScoreWeight::getSource, Function.identity()));
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        for (String source : SOURCES) {
            ScoreWeight weight = current.computeIfAbsent(source, s -> {
                ScoreWeight created = new ScoreWeight();
                created.setSource(s);
                return created;
            });
            BigDecimal next = requested.get(source);
            boolean changed = changeLog.record(ChangeEntity.SCORE_WEIGHT, source, "weight", weight.getWeight(), next,
                    request.reason(), adminId);
            if (changed || weight.getWeight() == null) {
                weight.setWeight(next);
                weight.setUpdatedBy(adminId);
                weight.setUpdatedAt(now);
            }
        }
        return view(weights.saveAll(current.values()));
    }

    private static ScoreWeightsView view(List<ScoreWeight> rows) {
        List<ScoreWeight> ordered = rows.stream()
                .sorted(Comparator.comparingInt(w -> SOURCES.indexOf(w.getSource())))
                .toList();
        BigDecimal total = ordered.stream().map(ScoreWeight::getWeight).reduce(BigDecimal.ZERO, BigDecimal::add);
        Instant updatedAt = ordered.stream().map(ScoreWeight::getUpdatedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(null);
        return new ScoreWeightsView(ordered.stream().map(w -> new ScoreWeightItem(w.getSource(), w.getWeight())).toList(),
                total, updatedAt);
    }
}
