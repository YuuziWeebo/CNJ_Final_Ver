package com.glucoze.thesismanagement.common.enums;

public enum ScoreSource {
    SUPERVISOR("Điểm GVHD"),
    COUNCIL("Điểm hội đồng");

    private final String displayName;

    ScoreSource(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
