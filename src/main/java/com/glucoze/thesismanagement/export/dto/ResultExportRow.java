package com.glucoze.thesismanagement.export.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ResultExportRow(String studentName, String thesisTitle, String supervisorName, String councilName,
                              BigDecimal finalScore, LocalDateTime defenseTime,
                              String academicYear, String semester) { }
