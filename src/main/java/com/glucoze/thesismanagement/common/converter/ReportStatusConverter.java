package com.glucoze.thesismanagement.common.converter;

import com.glucoze.thesismanagement.common.enums.ReportStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ReportStatusConverter implements AttributeConverter<ReportStatus, String> {
    @Override
    public String convertToDatabaseColumn(ReportStatus value) {
        if (value == null) return null;
        return switch (value) {
            case DRAFT -> "NHAP";
            case SUBMITTED -> "DA_NOP";
            case REVISION_REQUIRED -> "CAN_CHINH_SUA";
            case APPROVED -> "DA_DUYET";
        };
    }

    @Override
    public ReportStatus convertToEntityAttribute(String value) {
        if (value == null) return null;
        return switch (value) {
            case "NHAP" -> ReportStatus.DRAFT;
            case "DA_NOP" -> ReportStatus.SUBMITTED;
            case "CAN_CHINH_SUA" -> ReportStatus.REVISION_REQUIRED;
            case "DA_DUYET" -> ReportStatus.APPROVED;
            default -> throw new IllegalArgumentException("Unknown persisted ReportStatus value: " + value);
        };
    }
}
