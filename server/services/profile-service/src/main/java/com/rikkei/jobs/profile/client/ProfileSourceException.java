package com.rikkei.jobs.profile.client;

import lombok.Getter;

@Getter
public class ProfileSourceException extends RuntimeException {
    private final int status;
    private final String code;
    public ProfileSourceException(int status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    static ProfileSourceException unavailable() {
        return new ProfileSourceException(503, "EXTERNAL_UNAVAILABLE", "Không kết nối được dịch vụ hồ sơ nguồn. Vui lòng thử lại.");
    }
    static ProfileSourceException invalid() {
        return new ProfileSourceException(502, "EXTERNAL_INVALID_RESPONSE", "Dữ liệu hồ sơ nguồn không hợp lệ.");
    }
}
