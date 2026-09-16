package com.glucoze.thesismanagement.thesis.entity;

import com.glucoze.thesismanagement.common.BaseEntity;
import com.glucoze.thesismanagement.common.enums.ReportStatus;
import com.glucoze.thesismanagement.common.converter.ReportStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bao_cao")
public class Report extends BaseEntity {

    @OneToOne(optional = false)
    @JoinColumn(name = "dang_ky_id", nullable = false, unique = true)
    private Registration registration;

    @Column(name = "tep_bao_cao", length = 500)
    private String reportFile;

    @jakarta.persistence.Convert(converter = ReportStatusConverter.class)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private ReportStatus status = ReportStatus.DRAFT;

    @Column(name = "nhan_xet", columnDefinition = "TEXT")
    private String feedback;

    protected Report() {
    }

    public Report(Registration registration) {
        this.registration = registration;
    }

    public Registration getRegistration() {
        return registration;
    }

    public String getReportFile() {
        return reportFile;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public String getFeedback() {
        return feedback;
    }

    public void updateSubmission(String reportFile) {
        this.reportFile = reportFile;
    }

    public void addReview(String feedback) {
        this.feedback = feedback;
    }

    public void transitionTo(ReportStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Chuyển trạng thái báo cáo không hợp lệ: " + status + " -> " + target);
        }
        status = target;
    }
}
