package com.swefton.backend.modules.document.persistence;

import java.util.Locale;

import com.swefton.backend.modules.document.enums.DocumentType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DocumentTypeConverter implements AttributeConverter<String, String> {

    private static final String LEGACY_LICENSE = "LICENSE";

    @Override
    public String convertToDatabaseColumn(String type) {
        if (type == null) {
            return null;
        }
        return normalize(type);
    }

    @Override
    public String convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }

        return normalize(value);
    }

    private String normalize(String value) {
        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        return LEGACY_LICENSE.equals(normalizedValue)
                ? DocumentType.LICENCE
                : normalizedValue;
    }
}
