package com.glucoze.thesismanagement.home.dto;

import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.enums.ReportStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StudentDashboardView(String thesisTitle, RegistrationStatus registrationStatus,
                                   ReportStatus reportStatus, LocalDateTime defenseStart,
                                   String defenseRoom, String councilName,
                                   boolean resultPublished, BigDecimal finalScore) {
    public boolean hasRegistration() { return registrationStatus != null; }
    public boolean hasReport() { return reportStatus != null; }
    public boolean hasSchedule() { return defenseStart != null; }
}
