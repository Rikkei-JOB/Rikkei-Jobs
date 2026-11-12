package com.rikkei.jobs.profile.service.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rikkei.jobs.profile.entity.MappingSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Chuyển phản hồi của mock (docs/mock-contract.md) thành {@link SourceRecord}. Ánh xạ sang bằng chứng HS-01:
 * <ul>
 *   <li>LMS: mỗi môn là một bản ghi; source_code = subjectCode, điểm {@code score} thang 10. attendanceRate,
 *       homeworkRate, rpoint không phải điểm môn nên chỉ nằm trong payload.</li>
 *   <li>EXAM: mỗi bài thi là một bản ghi; source_code = subjectCode, ngày = takenAt.</li>
 *   <li>PM: mỗi đợt thực tập là một bản ghi; source_code = batchId, điểm = finalScore, ngày = lần nghiệm thu task
 *       cuối (hoặc endDate). Đợt chưa có finalScore là chưa chốt nên bỏ qua.</li>
 * </ul>
 * Bản ghi thiếu mã, điểm ngoài thang 0–10 hoặc ngày sai định dạng bị bỏ qua và được đếm vào {@code skipped}.
 */
@Component
@RequiredArgsConstructor
public class SourceRecordMapper {
    static final BigDecimal SCALE_MAX = BigDecimal.TEN;
    private static final ZoneId SOURCE_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };

    private final ObjectMapper json;

    public record Mapped(List<SourceRecord> records, int skipped) { }

    public Mapped map(String lmsUserId, List<JsonNode> lms, List<JsonNode> exam, List<JsonNode> pm) {
        Map<String, SourceRecord> byKey = new LinkedHashMap<>();
        int skipped = 0;
        for (JsonNode item : lms) {
            skipped += add(byKey, lms(lmsUserId, item));
        }
        for (JsonNode item : exam) {
            skipped += add(byKey, exam(lmsUserId, item));
        }
        for (JsonNode item : pm) {
            skipped += add(byKey, internship(lmsUserId, item));
        }
        return new Mapped(new ArrayList<>(byKey.values()), skipped);
    }

    /** Trả 1 nếu bản ghi bị bỏ qua (không hợp lệ hoặc trùng khoá trong cùng phản hồi; bản sau thắng). */
    private static int add(Map<String, SourceRecord> byKey, SourceRecord record) {
        if (record == null) {
            return 1;
        }
        return byKey.put(record.source() + "|" + record.sourceKey(), record) == null ? 0 : 1;
    }

    private SourceRecord lms(String lmsUserId, JsonNode item) {
        String code = code(item, "subjectCode");
        BigDecimal score = score(item.get("score"));
        if (code == null || score == null) {
            return null;
        }
        return new SourceRecord(MappingSource.LMS, lmsUserId + ":" + code, code,
                title(text(item, "subjectName"), code), score, SCALE_MAX, null, payload(item));
    }

    private SourceRecord exam(String lmsUserId, JsonNode item) {
        String examCode = code(item, "examCode");
        String subjectCode = code(item, "subjectCode");
        BigDecimal score = score(item.get("score"));
        Instant takenAt = instant(text(item, "takenAt"));
        if (examCode == null || subjectCode == null || score == null || takenAt == null) {
            return null;
        }
        return new SourceRecord(MappingSource.EXAM, lmsUserId + ":" + examCode, subjectCode,
                title("Bài thi " + examCode, examCode), score, SCALE_MAX, takenAt, payload(item));
    }

    private SourceRecord internship(String lmsUserId, JsonNode item) {
        String batchId = code(item, "batchId");
        BigDecimal score = score(item.get("finalScore"));
        Instant finishedAt = lastAcceptedAt(item);
        if (batchId == null || score == null || finishedAt == null) {
            return null;
        }
        String batchName = text(item, "batchName");
        String topic = text(item, "topicName");
        String title = (batchName == null ? batchId : batchName) + (topic == null ? "" : " – " + topic);
        return new SourceRecord(MappingSource.PM, lmsUserId + ":" + batchId, batchId,
                title(title, batchId), score, SCALE_MAX, finishedAt, payload(item));
    }

    private static Instant lastAcceptedAt(JsonNode internship) {
        Instant last = null;
        JsonNode tasks = internship.get("tasks");
        if (tasks != null && tasks.isArray()) {
            for (JsonNode task : tasks) {
                Instant acceptedAt = instant(text(task, "acceptedAt"));
                if (acceptedAt != null && (last == null || acceptedAt.isAfter(last))) {
                    last = acceptedAt;
                }
            }
        }
        if (last == null) {
            String endDate = text(internship, "endDate");
            try {
                last = endDate == null ? null : LocalDate.parse(endDate).atStartOfDay(SOURCE_ZONE).toInstant();
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return last;
    }

    private Map<String, Object> payload(JsonNode item) {
        return json.convertValue(item, MAP);
    }

    private static String text(JsonNode item, String field) {
        JsonNode value = item == null ? null : item.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            return null;
        }
        return value.asText().trim();
    }

    private static String code(JsonNode item, String field) {
        String value = text(item, field);
        return value == null || value.length() > 100 ? null : value;
    }

    private static String title(String preferred, String fallback) {
        String title = preferred == null ? fallback : preferred;
        return title.length() > 255 ? title.substring(0, 255) : title;
    }

    private static BigDecimal score(JsonNode value) {
        if (value == null || !value.isNumber()) {
            return null;
        }
        BigDecimal score = value.decimalValue();
        if (score.signum() < 0 || score.compareTo(SCALE_MAX) > 0 || score.stripTrailingZeros().scale() > 12) {
            return null;
        }
        return score;
    }

    private static Instant instant(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toInstant().truncatedTo(ChronoUnit.MICROS);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
