package com.glucoze.thesismanagement.common.enums;

public enum RegistrationStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Từ chối"),
    COMPLETED("Hoàn thành"),
    CANCELLED("Đã hủy");

    private final String displayName;

    RegistrationStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean canTransitionTo(RegistrationStatus target) {
        return switch (this) {
            case PENDING -> target == APPROVED || target == REJECTED || target == CANCELLED;
            case APPROVED -> target == COMPLETED || target == CANCELLED;
            case REJECTED, COMPLETED, CANCELLED -> false;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
