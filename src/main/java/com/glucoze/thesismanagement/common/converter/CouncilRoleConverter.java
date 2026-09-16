package com.glucoze.thesismanagement.common.converter;

import com.glucoze.thesismanagement.common.enums.CouncilRole;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CouncilRoleConverter implements AttributeConverter<CouncilRole, String> {
    @Override
    public String convertToDatabaseColumn(CouncilRole value) {
        if (value == null) return null;
        return switch (value) {
            case CHAIR -> "CHU_TICH";
            case SECRETARY -> "THU_KY";
            case MEMBER -> "UY_VIEN";
        };
    }

    @Override
    public CouncilRole convertToEntityAttribute(String value) {
        if (value == null) return null;
        return switch (value) {
            case "CHU_TICH" -> CouncilRole.CHAIR;
            case "THU_KY" -> CouncilRole.SECRETARY;
            case "UY_VIEN" -> CouncilRole.MEMBER;
            default -> throw new IllegalArgumentException("Unknown persisted CouncilRole value: " + value);
        };
    }
}
