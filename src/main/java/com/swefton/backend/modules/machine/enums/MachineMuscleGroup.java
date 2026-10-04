package com.swefton.backend.modules.machine.enums;

public enum MachineMuscleGroup {
    CHEST("Chest"),
    BACK("Back"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    ABS("Abs"),
    LEGS("Legs"),
    GLUTES("Glutes"),
    CALVES("Calves"),
    CARDIO("Cardio");
    

    private final String label;

    MachineMuscleGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
