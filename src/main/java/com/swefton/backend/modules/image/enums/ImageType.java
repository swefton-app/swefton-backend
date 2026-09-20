package com.swefton.backend.modules.image.enums;

import java.util.Locale;
import java.util.Set;

public final class ImageType {

    public static final String PROFILE = "PROFILE";
    public static final String COVER = "COVER";
    public static final String LOGO = "LOGO";
    public static final String GALLERY = "GALLERY";

    private static final Set<String> VALUES = Set.of(PROFILE, COVER, LOGO, GALLERY);

    private ImageType() {
    }

    public static boolean exists(String value) {
        return value != null && VALUES.contains(value);
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
