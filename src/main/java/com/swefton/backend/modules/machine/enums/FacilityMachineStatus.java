package com.swefton.backend.modules.machine.enums;

public enum FacilityMachineStatus {
    ACTIVE("Active"),
    COMING_SOON("Coming Soon"),
    NEEDS_REPAIR("Needs Repair"),
    UNDER_REPAIR("Under Repair"),
    OUT_OF_SERVICE("Out of Service");

    private final String label;

    FacilityMachineStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
