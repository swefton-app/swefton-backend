package com.swefton.backend.modules.facility.enums;

import java.util.Set;

public final class FacilityCategory {

    public static final String GYM = "GYM";
    public static final String SWIMMING = "SWIMMING";
    public static final String BOXING = "BOXING";
    public static final String MARTIAL_ARTS = "MARTIAL_ARTS";
    public static final String YOGA = "YOGA";
    public static final String CROSSFIT = "CROSSFIT";

    private static final Set<String> VALUES = Set.of(
            GYM,
            SWIMMING,
            BOXING,
            MARTIAL_ARTS,
            YOGA,
            CROSSFIT
    );

    private FacilityCategory() {
    }

    public static boolean exists(String value) {
        return value != null && VALUES.contains(value);
    }
}
