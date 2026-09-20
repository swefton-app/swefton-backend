package com.swefton.backend.modules.document.enums;

import java.util.Locale;
import java.util.Set;

public final class DocumentType {

    public static final String LICENCE = "LICENCE";
    public static final String LICENSE = "LICENSE";
    public static final String CV = "CV";
    public static final String OTHER = "OTHER";

    private static final Set<String> VALUES = Set.of(LICENCE, CV, OTHER);

    private DocumentType() {
    }

    public static boolean exists(String value) {
        return value != null && VALUES.contains(normalize(value));
    }

    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return LICENSE.equals(normalized) ? LICENCE : normalized;
    }
}
