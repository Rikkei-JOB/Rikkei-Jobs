package com.rikkei.jobs.profile.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình NT-05 ({@code app.sync.*} trong application.yml). */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.sync")
public class SyncProperties {
    /** Địa chỉ mock hệ thống ngoài (LMS, Khảo thí, lms-pm). */
    private String baseUrl = "http://localhost:9000";
    /** Giá trị header X-Service-Key. */
    private String serviceKey = "dev-key";
    /** Timeout kết nối và đọc của mỗi request. */
    private Duration timeout = Duration.ofSeconds(5);
    /** Số lần thử lại khi lỗi mạng hoặc 5xx (không tính lần gọi đầu). */
    private int retries = 2;
    /** Thời gian chờ giữa hai lần thử. */
    private Duration retryBackoff = Duration.ofMillis(500);
    /** Ứng viên mở hồ sơ mà lần đồng bộ cuối cũ hơn ngưỡng này thì đồng bộ nền. */
    private Duration staleAfter = Duration.ofHours(1);
    /** Bật đồng bộ nền khi ứng viên mở hồ sơ. */
    private boolean onOpenEnabled = true;
    /** Lịch job đêm (cron Spring, 6 trường); "-" để tắt. */
    private String nightlyCron = "0 0 1 * * *";
    /** Múi giờ của lịch job đêm. */
    private String zone = "Asia/Ho_Chi_Minh";
}
