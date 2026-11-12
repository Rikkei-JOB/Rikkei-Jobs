package com.rikkei.jobs.company.client;

import com.rikkei.jobs.common.config.security.principle.Role;
import java.util.UUID;

/** identity-service báo email đã có tài khoản; role/companyId có thể null (ví dụ ứng viên không có công ty). */
public class EmailExistsException extends RuntimeException {

    private final UUID userId;
    private final Role role;
    private final UUID companyId;

    public EmailExistsException(UUID userId, Role role, UUID companyId) {
        super("Email đã có tài khoản");
        this.userId = userId;
        this.role = role;
        this.companyId = companyId;
    }

    public UUID userId() {
        return userId;
    }

    public Role role() {
        return role;
    }

    public UUID companyId() {
        return companyId;
    }
}
