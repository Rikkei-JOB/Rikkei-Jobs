package com.rikkei.jobs.company.client;

import java.util.UUID;

/** Một kỹ năng trong danh mục của profile-service; name đã thay bằng code nếu rỗng. */
public record CatalogSkill(UUID id, String code, String name) {
}
