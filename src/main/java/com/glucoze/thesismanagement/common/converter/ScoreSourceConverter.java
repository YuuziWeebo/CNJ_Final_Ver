package com.glucoze.thesismanagement.common.converter;

import com.glucoze.thesismanagement.common.enums.ScoreSource;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ScoreSourceConverter implements AttributeConverter<ScoreSource, String> {
    @Override
    public String convertToDatabaseColumn(ScoreSource value) {
        if (value == null) return null;
        return switch (value) {
            case SUPERVISOR -> "GVHD";
            case COUNCIL -> "HOI_DONG";
        };
    }

    @Override
    public ScoreSource convertToEntityAttribute(String value) {
        if (value == null) return null;
        return switch (value) {
            case "GVHD" -> ScoreSource.SUPERVISOR;
            case "HOI_DONG" -> ScoreSource.COUNCIL;
            default -> throw new IllegalArgumentException("Unknown persisted ScoreSource value: " + value);
        };
    }
}
