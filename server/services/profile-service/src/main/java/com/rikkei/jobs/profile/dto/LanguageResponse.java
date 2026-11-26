package com.rikkei.jobs.profile.dto;

import com.rikkei.jobs.profile.entity.Language;
import java.util.UUID;

public record LanguageResponse(UUID id, String name, Language.Level level, String note) {
    public static LanguageResponse from(Language l) { return new LanguageResponse(l.getId(), l.getName(), l.getLevel(), l.getNote()); }
}
