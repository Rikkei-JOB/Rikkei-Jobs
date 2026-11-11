package com.rikkei.jobs.common.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/** Danh sách phân trang theo quy ước nhóm: {@code {items, total, page, size}}, page bắt đầu từ 1. */
public record PageResponse<T>(List<T> items, long total, int page, int size) {

    public static <T, U> PageResponse<T> of(Page<U> page, List<T> mappedItems) {
        return new PageResponse<>(mappedItems, page.getTotalElements(), page.getNumber() + 1, page.getSize());
    }
}
