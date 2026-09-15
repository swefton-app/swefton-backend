package com.swefton.backend.modules.document.enums;

public enum DocumentType {
    LICENCE,
    /**
     * Legacy database/API spelling kept so schema validation accepts existing rows.
     * New application code uses {@link #LICENCE}.
     */
    @Deprecated
    LICENSE,
    CV,
    OTHER
}
