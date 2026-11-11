package com.rikkei.jobs.common.config.security.principle;

/**
 * Guard theo vai trò: biểu thức dùng sẵn cho {@code @PreAuthorize}, đặt trên từng method của controller.
 *
 * <pre>{@code
 * @PreAuthorize(Roles.HR)                          // chỉ HR
 * @PreAuthorize(Roles.HR_STAFF_ADMIN)              // HR, STAFF hoặc ADMIN
 * @PreAuthorize("hasAnyRole('MENTOR','ADMIN')")    // tổ hợp khác
 * }</pre>
 *
 * Sai vai trò → 403 {@code FORBIDDEN}; thiếu hoặc sai token → 401 {@code UNAUTHORIZED}.
 * Quyền theo dữ liệu (công ty của HR, dữ liệu của ứng viên) kiểm thêm bằng
 * {@link com.rikkei.jobs.common.config.security.access.OwnershipGuard} và trả 404.
 */
public final class Roles {

    public static final String CANDIDATE = "hasRole('CANDIDATE')";
    public static final String HR = "hasRole('HR')";
    public static final String MENTOR = "hasRole('MENTOR')";
    public static final String STAFF = "hasRole('STAFF')";
    public static final String ADMIN = "hasRole('ADMIN')";
    /** Cán bộ tuyển dụng hoặc quản trị. */
    public static final String STAFF_OR_ADMIN = "hasAnyRole('STAFF','ADMIN')";
    /** HR doanh nghiệp, cán bộ tuyển dụng hoặc quản trị (dữ liệu doanh nghiệp; HR chỉ của công ty mình). */
    public static final String HR_STAFF_ADMIN = "hasAnyRole('HR','STAFF','ADMIN')";
    /** Mọi vai trò đã đăng nhập. */
    public static final String AUTHENTICATED = "isAuthenticated()";

    private Roles() {
    }
}
