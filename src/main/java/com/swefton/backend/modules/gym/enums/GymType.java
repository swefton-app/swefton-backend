package com.swefton.backend.modules.gym.enums;

import java.util.Set;

public final class GymType {

    public static final String COMMERCIAL = "COMMERCIAL";
    public static final String BOUTIQUE = "BOUTIQUE";
    public static final String BODYBUILDING = "BODYBUILDING";
    public static final String CROSSFIT = "CROSSFIT";
    public static final String POWERLIFTING = "POWERLIFTING";
    public static final String FUNCTIONAL_TRAINING = "FUNCTIONAL_TRAINING";
    public static final String WOMEN_ONLY = "WOMEN_ONLY";
    public static final String OTHER = "OTHER";

    private static final Set<String> VALUES = Set.of(
            COMMERCIAL,
            BOUTIQUE,
            BODYBUILDING,
            CROSSFIT,
            POWERLIFTING,
            FUNCTIONAL_TRAINING,
            WOMEN_ONLY,
            OTHER);

    private GymType() {
    }

    public static boolean exists(String value) {
        return value != null && VALUES.contains(value);
    }
}
