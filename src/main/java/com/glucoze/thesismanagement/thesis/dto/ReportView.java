package com.glucoze.thesismanagement.thesis.dto;

import com.glucoze.thesismanagement.common.enums.ReportStatus;

public record ReportView(Long id, String thesisTitle, String studentName, String file,
                         ReportStatus status, String review) {
}