package com.swefton.backend.modules.membership.enums;

import java.util.Locale;

public enum AccessDirection {
    ENTER,
    EXIT;

    public static AccessDirection from(String value) {
        return AccessDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
