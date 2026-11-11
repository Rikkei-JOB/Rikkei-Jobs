package com.rikkei.jobs.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Body lỗi theo quy ước nhóm: hai field bắt buộc {@code code} và {@code message};
 * {@code errors} chỉ có khi cần chi tiết (lỗi từng field, số phút còn bị khoá...).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String code, String message, Object errors) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }

    public static ErrorResponse of(String code, String message, Object errors) {
        return new ErrorResponse(code, message, errors);
    }
}
