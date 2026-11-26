package com.rikkei.jobs.identity.service;

import com.rikkei.jobs.common.config.exception.BadRequestException;
import com.rikkei.jobs.common.config.exception.ConflictException;
import com.rikkei.jobs.common.config.exception.ResourceNotFoundException;
import com.rikkei.jobs.common.config.security.principle.Role;
import com.rikkei.jobs.common.dto.response.PageResponse;
import com.rikkei.jobs.identity.dto.AdminUserDtos.AdminUserItem;
import com.rikkei.jobs.identity.dto.AdminUserDtos.AdminUserSummary;
import com.rikkei.jobs.identity.entity.User;
import com.rikkei.jobs.identity.entity.UserStatus;
import com.rikkei.jobs.identity.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản trị tài khoản: xem mọi tài khoản, khoá / mở khoá kèm lý do.
 * Khoá có hiệu lực ngay: đăng nhập mật khẩu, SSO và mọi request tới identity đều bị chặn (isAdminLocked);
 * company-service còn chặn HR qua HrLockedFilter. Không cho khoá chính mình hoặc tài khoản quản trị khác (tránh khoá mất quyền quản trị).
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository users;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserItem> list(String q, Role role, String status, int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw new BadRequestException("Phân trang không hợp lệ.");
        if (status != null && !status.equals("ACTIVE") && !status.equals("LOCKED")) throw new BadRequestException("Trạng thái không hợp lệ.");
        String pattern = q == null || q.isBlank() ? null
            : "%" + q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            if (pattern != null) all.add(cb.or(
                cb.like(cb.lower(root.get("email")), pattern, '\\'),
                cb.like(cb.lower(root.get("fullName")), pattern, '\\'),
                cb.like(cb.lower(cb.coalesce(root.get("lmsUserId"), "")), pattern, '\\')));
            if (role != null) all.add(cb.equal(root.get("role"), role));
            if ("LOCKED".equals(status)) all.add(cb.isNotNull(root.get("lockedAt")));
            if ("ACTIVE".equals(status)) all.add(cb.isNull(root.get("lockedAt")));
            return cb.and(all.toArray(Predicate[]::new));
        };
        Page<User> result = users.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("email"))));
        return PageResponse.of(result, result.getContent().stream().map(AdminUserItem::from).toList());
    }

    @Transactional(readOnly = true)
    public AdminUserSummary summary() {
        Map<Role, Long> byRole = new EnumMap<>(Role.class);
        for (Role role : Role.values()) byRole.put(role, 0L);
        long total = 0;
        for (Object[] row : users.countByRole()) {
            long count = ((Number) row[1]).longValue();
            byRole.put((Role) row[0], count);
            total += count;
        }
        return new AdminUserSummary(total, users.countByLockedAtIsNotNull(), byRole);
    }

    @Transactional
    public AdminUserItem lock(UUID adminId, UUID id, String reason) {
        if (adminId.equals(id)) throw new ConflictException("Không thể tự khoá tài khoản của mình.");
        User user = find(id);
        if (user.getRole() == Role.ADMIN) throw new ConflictException("Không thể khoá tài khoản quản trị khác.");
        Instant now = clock.instant();
        user.setStatus(UserStatus.LOCKED);
        user.setLockedReason(reason);
        user.setLockedAt(now);
        return AdminUserItem.from(users.save(user));
    }

    @Transactional
    public AdminUserItem unlock(UUID id) {
        User user = find(id);
        if (user.isAdminLocked()) {
            user.setStatus(UserStatus.ACTIVE);
            user.setLockedReason(null);
            user.setLockedAt(null);
            user.setLockedUntil(null);
            user.setFailedLoginCount(0);
            user = users.save(user);
        }
        return AdminUserItem.from(user);
    }

    private User find(UUID id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
    }
}
