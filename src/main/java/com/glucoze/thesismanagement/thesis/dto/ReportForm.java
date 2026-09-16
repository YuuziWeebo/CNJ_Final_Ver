package com.glucoze.thesismanagement.thesis.dto;

import jakarta.validation.constraints.Size;
import com.glucoze.thesismanagement.thesis.validation.ValidExternalReportUrl;

public class ReportForm {

    @Size(max = 500)
    @ValidExternalReportUrl
    private String reportFile;

    public ReportForm() {
    }

    public ReportForm(String reportFile) {
        this.reportFile = reportFile;
    }

    public String getReportFile() {
        return reportFile;
    }

    public void setReportFile(String reportFile) {
        this.reportFile = reportFile;
    }
}
