package com.rikkei.jobs.company.util;

/** Quy tắc dữ liệu công ty dùng chung cho validation và kiểm tra tệp. */
public final class CompanyRules {

    public static final String TAX_CODE_REGEX = "^(\\d{10}|\\d{13})$";
    public static final String TAX_CODE_MESSAGE = "Mã số thuế phải gồm 10 hoặc 13 chữ số";
    public static final String SIZE_REGEX = "^$|^(1-50|51-200|201-1000|1000\\+)$";
    public static final String WEBSITE_REGEX = "^$|^https?://\\S+$";
    public static final long LOGO_MAX_BYTES = 2 * 1024 * 1024;

    private CompanyRules() {
    }
}
