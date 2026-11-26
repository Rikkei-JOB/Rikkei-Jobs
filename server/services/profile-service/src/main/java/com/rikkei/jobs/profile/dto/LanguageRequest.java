package com.rikkei.jobs.profile.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.rikkei.jobs.profile.entity.Language;
import jakarta.validation.constraints.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LanguageRequest(@NotBlank @Size(max = 100) String name, @NotNull Language.Level level, @Size(max = 255) String note) {
    public LanguageRequest {
        name = trim(name); note = trim(note);
    }
    private static String trim(String value) { return value == null || value.isBlank() ? null : value.strip(); }
}
