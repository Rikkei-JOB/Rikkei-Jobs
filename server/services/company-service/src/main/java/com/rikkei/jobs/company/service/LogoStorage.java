package com.rikkei.jobs.company.service;

import java.util.Optional;
import org.springframework.core.io.Resource;

/** Nơi lưu tệp logo công ty. */
public interface LogoStorage {

    /** Ghi tệp mới và trả về tên tệp dạng {@code <uuid>.<extension>}. */
    String store(byte[] content, String extension);

    /** Tên không khớp {@code ^[0-9a-f-]{36}\.(jpg|png)$} hoặc tệp không có thì trả về rỗng. */
    Optional<Resource> load(String fileName);

    /** Xoá tệp; không ném lỗi khi tệp không còn hoặc tên không hợp lệ. */
    void delete(String fileName);
}
