package com.glucoze.thesismanagement.common.converter;

import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RegistrationStatusConverter implements AttributeConverter<RegistrationStatus, String> {
    @Override
    public String convertToDatabaseColumn(RegistrationStatus value) {
        if (value == null) return null;
        return switch (value) {
            case PENDING -> "CHO_DUYET";
            case APPROVED -> "DA_DUYET";
            case REJECTED -> "TU_CHOI";
            case COMPLETED -> "HOAN_THANH";
            case CANCELLED -> "HUY";
        };
    }

    @Override
    public RegistrationStatus convertToEntityAttribute(String value) {
        if (value == null) return null;
        return switch (value) {
            case "CHO_DUYET" -> RegistrationStatus.PENDING;
            case "DA_DUYET" -> RegistrationStatus.APPROVED;
            case "TU_CHOI" -> RegistrationStatus.REJECTED;
            case "HOAN_THANH" -> RegistrationStatus.COMPLETED;
            case "HUY" -> RegistrationStatus.CANCELLED;
            default -> throw new IllegalArgumentException("Unknown persisted RegistrationStatus value: " + value);
        };
    }
}
