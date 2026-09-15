package com.swefton.backend.modules.document.persistence;

import java.util.Locale;

import com.swefton.backend.modules.document.enums.DocumentType;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class DocumentTypeConverter implements AttributeConverter<DocumentType, String> {

    private static final String LEGACY_LICENSE = "LICENSE";

    @Override
    public String convertToDatabaseColumn(DocumentType type) {
        if (type == null) {
            return null;
        }
        return type.name();
    }

    @Override
    public DocumentType convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        if (LEGACY_LICENSE.equals(normalizedValue)) {
            return DocumentType.LICENCE;
        }
        return DocumentType.valueOf(normalizedValue);
    }
}
