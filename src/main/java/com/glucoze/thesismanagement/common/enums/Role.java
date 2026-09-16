package com.glucoze.thesismanagement.common.enums;

public enum Role {
    STUDENT("Sinh viên"),
    LECTURER("Giảng viên"),
    ADMIN("Quản trị viên");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
