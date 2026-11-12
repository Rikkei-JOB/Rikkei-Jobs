package com.rikkei.jobs.common.config.security.access;

import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.AuthPrincipal;
import com.rikkei.jobs.common.config.security.principle.Role;

import java.util.Objects;
import java.util.UUID;

/**
 * Kiểm quyền sở hữu dữ liệu sau khi đã qua kiểm vai trò ({@code @PreAuthorize}).
 *
 * <p>Dữ liệu không thuộc người gọi bị coi như <b>không tồn tại</b>: ném {@link ResourceNotFoundException} (404),
 * cùng thông báo với khi id không có thật, để không dò được id của người khác. Không trả 403 ở đây.
 *
 * <p>Cán bộ tuyển dụng ({@code STAFF}) và quản trị ({@code ADMIN}) xem được dữ liệu của mọi công ty, mọi người dùng.
 * Task tính năng nào cần hẹp hơn thì tự kiểm thêm.
 *
 * <pre>{@code
 * Company company = repository.findById(id).orElseThrow(() -> OwnershipGuard.notFound("Không tìm thấy công ty"));
 * OwnershipGuard.requireCompany(principal, company.getId(), "Không tìm thấy công ty");
 * }</pre>
 *
 * Khi có thể, lọc ngay trong truy vấn (ví dụ {@code findByIdAndCompanyId}) thay vì đọc lên rồi mới so.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {
    }

    /** STAFF và ADMIN không bị giới hạn theo công ty hay theo chủ dữ liệu. */
    public static boolean seesAllData(AuthPrincipal principal) {
        Role role = principal.getRole();
        return role == Role.STAFF || role == Role.ADMIN;
    }

    /** HR chỉ thao tác được dữ liệu có {@code company_id} trùng công ty trong JWT. */
    public static boolean ownsCompany(AuthPrincipal principal, UUID resourceCompanyId) {
        return seesAllData(principal)
                || (principal.getRole() == Role.HR
                        && principal.getCompanyId() != null
                        && Objects.equals(principal.getCompanyId(), resourceCompanyId));
    }

    /** Người dùng (ví dụ ứng viên) chỉ thao tác được dữ liệu mà mình là chủ. */
    public static boolean ownsUserData(AuthPrincipal principal, UUID resourceOwnerUserId) {
        return seesAllData(principal)
                || (principal.getUserId() != null && Objects.equals(principal.getUserId(), resourceOwnerUserId));
    }

    /** Ném 404 nếu người gọi không thuộc công ty sở hữu dữ liệu. */
    public static void requireCompany(AuthPrincipal principal, UUID resourceCompanyId, String notFoundMessage) {
        if (!ownsCompany(principal, resourceCompanyId)) {
            throw notFound(notFoundMessage);
        }
    }

    /** Ném 404 nếu người gọi không phải chủ dữ liệu. */
    public static void requireOwner(AuthPrincipal principal, UUID resourceOwnerUserId, String notFoundMessage) {
        if (!ownsUserData(principal, resourceOwnerUserId)) {
            throw notFound(notFoundMessage);
        }
    }

    /** Dùng chung cho "không có" và "không phải của bạn", để hai trường hợp trả về giống hệt nhau. */
    public static ResourceNotFoundException notFound(String message) {
        return new ResourceNotFoundException(message);
    }
}
