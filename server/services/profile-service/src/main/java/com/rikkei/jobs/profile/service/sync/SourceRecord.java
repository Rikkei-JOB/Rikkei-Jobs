package com.rikkei.jobs.profile.service.sync;

import com.rikkei.jobs.profile.entity.MappingSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Một kết quả đã chốt lấy từ hệ thống ngoài, sẵn sàng ghi vào external_records.
 * {@code recordedAt} null khi nguồn không có ngày (LMS): dùng thời điểm đồng bộ đầu tiên hoặc lúc điểm đổi.
 */
public record SourceRecord(MappingSource source, String sourceKey, String sourceCode, String title,
                           BigDecimal score, BigDecimal scoreMax, Instant recordedAt, Map<String, Object> payload) { }
