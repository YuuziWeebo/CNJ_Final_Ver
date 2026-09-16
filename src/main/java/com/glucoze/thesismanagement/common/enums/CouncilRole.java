package com.glucoze.thesismanagement.common.enums;

public enum CouncilRole {
    CHAIR("Chủ tịch"),
    SECRETARY("Thư ký"),
    MEMBER("Ủy viên");

    private final String displayName;

    CouncilRole(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
