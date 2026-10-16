package com.swefton.backend.modules.gym.enums;

import java.util.Set;

public final class GymStatus {

    public static final String DRAFT = "DRAFT";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    private static final Set<String> VALUES = Set.of(DRAFT, PENDING_APPROVAL, ACTIVE, INACTIVE);

    private GymStatus() {
    }

    public static boolean exists(String value) {
        return value != null && VALUES.contains(value);
    }
}
