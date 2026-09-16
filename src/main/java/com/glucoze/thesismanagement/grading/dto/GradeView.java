package com.glucoze.thesismanagement.grading.dto;

import java.math.BigDecimal;

public record GradeView(Long scheduleId, String thesisTitle, BigDecimal supervisorScore,
                        BigDecimal councilScore, BigDecimal finalScore, boolean published) {
}