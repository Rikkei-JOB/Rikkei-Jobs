package com.rikkei.jobs.profile.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OverrideFieldConverter implements AttributeConverter<OverrideField, String> {
    @Override
    public String convertToDatabaseColumn(OverrideField field) {
        return field == null ? null : field.key();
    }

    @Override
    public OverrideField convertToEntityAttribute(String key) {
        return key == null ? null : OverrideField.fromKey(key)
                .orElseThrow(() -> new IllegalStateException("Trường sửa tay không hợp lệ: " + key));
    }
}
