package com.rikkei.jobs.company.client;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Danh mục kỹ năng của profile-service, dùng để kiểm tra id kỹ năng của JD và hiển thị tên kỹ năng. */
public interface SkillCatalogClient {

    /** Gọi GET /skills với nguyên header Authorization của người dùng, trả danh sách kỹ năng. */
    List<CatalogSkill> skills(String authorization);

    /** Tập id kỹ năng của danh mục. */
    default Set<UUID> skillIds(String authorization) {
        Set<UUID> ids = new HashSet<>();
        for (CatalogSkill skill : skills(authorization)) {
            ids.add(skill.id());
        }
        return ids;
    }
}
