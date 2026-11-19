package com.rikkei.jobs.profile.repository.admin;

import com.rikkei.jobs.profile.entity.admin.ChangeEntity;
import com.rikkei.jobs.profile.entity.admin.ChangeLog;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChangeLogRepository extends JpaRepository<ChangeLog, UUID>, JpaSpecificationExecutor<ChangeLog> {

    /** Lọc tuỳ chọn theo loại, khoá, khoảng thời gian [from, to); mới nhất trước. */
    default Page<ChangeLog> search(ChangeEntity entity, String entityId, Instant from, Instant to, Pageable pageable) {
        Specification<ChangeLog> spec = (root, query, cb) -> cb.conjunction();
        if (entity != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entity"), entity));
        }
        if (entityId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("entityId"), entityId));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("createdAt"), to));
        }
        return findAll(spec, pageable);
    }
}
