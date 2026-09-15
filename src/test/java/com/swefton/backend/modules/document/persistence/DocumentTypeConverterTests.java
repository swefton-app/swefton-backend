package com.swefton.backend.modules.document.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.swefton.backend.modules.document.enums.DocumentType;

class DocumentTypeConverterTests {

    private final DocumentTypeConverter converter = new DocumentTypeConverter();

    @Test
    void readsLegacyAmericanLicenseSpelling() {
        assertThat(converter.convertToEntityAttribute("LICENSE"))
                .isEqualTo(DocumentType.LICENCE);
    }

    @Test
    void storesTheCanonicalDocumentTypeName() {
        assertThat(converter.convertToDatabaseColumn(DocumentType.LICENCE))
                .isEqualTo("LICENCE");
    }
}
