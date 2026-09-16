package com.glucoze.thesismanagement.thesis.dto;

import com.glucoze.thesismanagement.common.enums.ReportStatus;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;

public record RegistrationView(Long id, Long thesisId, String thesisTitle, String studentName,
                               RegistrationStatus status, ReportStatus reportStatus) {
}