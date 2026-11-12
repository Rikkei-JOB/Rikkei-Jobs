package com.rikkei.jobs.profile.client;

import com.rikkei.jobs.profile.entity.MappingSource;

/** Không lấy được dữ liệu từ một hệ thống ngoài; message là câu hiển thị cho cán bộ. */
public class ExternalSourceException extends RuntimeException {
    private final MappingSource source;

    public ExternalSourceException(MappingSource source, String message, Throwable cause) {
        super(message, cause);
        this.source = source;
    }

    public MappingSource getSource() {
        return source;
    }
}
