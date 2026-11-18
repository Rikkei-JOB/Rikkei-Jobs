package com.rikkei.jobs.company.client;

import java.util.Set;
import java.util.UUID;

/** Danh mục kỹ năng của profile-service, dùng để kiểm tra id kỹ năng của JD. */
public interface SkillCatalogClient {

    /** Gọi GET /skills với nguyên header Authorization của người dùng, trả tập id kỹ năng. */
    Set<UUID> skillIds(String authorization);
}
