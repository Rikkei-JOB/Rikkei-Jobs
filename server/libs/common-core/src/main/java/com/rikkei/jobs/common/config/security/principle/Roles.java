package com.rikkei.jobs.common.config.security.principle;

/** Biểu thức dùng sẵn cho {@code @PreAuthorize}, ví dụ {@code @PreAuthorize(Roles.HR)}. */
public final class Roles {

    public static final String CANDIDATE = "hasRole('CANDIDATE')";
    public static final String HR = "hasRole('HR')";
    public static final String MENTOR = "hasRole('MENTOR')";
    public static final String STAFF = "hasRole('STAFF')";
    public static final String ADMIN = "hasRole('ADMIN')";
    /** Cán bộ tuyển dụng hoặc quản trị. */
    public static final String STAFF_OR_ADMIN = "hasAnyRole('STAFF','ADMIN')";

    private Roles() {
    }
}
