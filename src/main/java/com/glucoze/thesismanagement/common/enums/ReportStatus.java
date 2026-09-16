package com.glucoze.thesismanagement.common.enums;

public enum ReportStatus {
    DRAFT("Nháp"),
    SUBMITTED("Đã nộp"),
    REVISION_REQUIRED("Cần chỉnh sửa"),
    APPROVED("Đã duyệt");

    private final String displayName;

    ReportStatus(String displayName) {
        this.displayName = displayName;
    }

    public boolean canTransitionTo(ReportStatus target) {
        return switch (this) {
            case DRAFT -> target == SUBMITTED;
            case SUBMITTED -> target == REVISION_REQUIRED || target == APPROVED;
            case REVISION_REQUIRED -> target == SUBMITTED;
            case APPROVED -> false;
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
