package com.glucoze.thesismanagement.thesis.service;

import com.glucoze.thesismanagement.common.enums.ReportStatus;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import com.glucoze.thesismanagement.thesis.dto.RegistrationView;
import com.glucoze.thesismanagement.thesis.dto.ReportForm;
import com.glucoze.thesismanagement.thesis.dto.ReportView;
import com.glucoze.thesismanagement.thesis.dto.ThesisForm;
import com.glucoze.thesismanagement.thesis.dto.ThesisView;
import com.glucoze.thesismanagement.thesis.entity.Report;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.thesis.repository.ReportRepository;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.thesis.validation.ExternalReportUrl;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-model boundary for thesis, registration and report screens. */
@Service
@Transactional(readOnly = true)
public class ThesisQueryService {

    private final ThesisRepository thesisRepository;
    private final RegistrationRepository registrationRepository;
    private final ReportRepository reportRepository;

    public ThesisQueryService(ThesisRepository thesisRepository,
                              RegistrationRepository registrationRepository,
                              ReportRepository reportRepository) {
        this.thesisRepository = thesisRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
    }

    public ThesisForm getThesisForm(String username, Long thesisId) {
        Thesis thesis = thesisRepository.findByIdAndSupervisorUserAccountUsername(thesisId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề tài"));
        return new ThesisForm(thesis.getTitle(), thesis.getDescription(), thesis.getAcademicYear(), thesis.getSemester());
    }

    public List<ThesisView> listLecturerTheses(String username) {
        return thesisRepository.findBySupervisorUserAccountUsername(username).stream()
                .map(this::toThesisView).toList();
    }

    public List<ThesisView> browseTheses(String academicYear, String semester) {
        List<Thesis> theses = academicYear == null || academicYear.isBlank() || semester == null || semester.isBlank()
                ? thesisRepository.findAll()
                : thesisRepository.findByAcademicYearAndSemester(academicYear.trim(), semester.trim());
        return theses.stream().map(this::toThesisView).toList();
    }

    public Long activeStudentThesisId(String username) {
        return registrationRepository.findByStudentUserAccountUsername(username).stream()
                .filter(Registration::isActive)
                .map(registration -> registration.getThesis().getId())
                .findFirst().orElse(null);
    }

    public List<RegistrationView> listStudentRegistrations(String username) {
        return toRegistrationViews(registrationRepository.findByStudentUserAccountUsername(username));
    }

    public List<RegistrationView> listLecturerRegistrations(String username) {
        return toRegistrationViews(
                registrationRepository.findByThesisSupervisorUserAccountUsername(username));
    }

    public ReportForm getReportForm(String username, Long registrationId) {
        Registration registration = registrationRepository.findByIdAndStudentUserAccountUsername(registrationId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký"));
        if (registration.getStatus() != RegistrationStatus.APPROVED) {
            throw new DomainRuleViolationException(
                    "Chỉ đăng ký đã duyệt mới được nộp báo cáo (approved registration required)");
        }
        return reportRepository.findByRegistrationId(registration.getId())
                .map(report -> new ReportForm(externalUrlOrNull(report.getReportFile())))
                .orElseGet(ReportForm::new);
    }

    public List<ReportView> listStudentReports(String username) {
        return reportRepository.findByRegistrationStudentUserAccountUsername(username).stream()
                .map(this::toReportView).toList();
    }

    public List<ReportView> listLecturerReports(String username) {
        return reportRepository.findByRegistrationThesisSupervisorUserAccountUsername(username).stream()
                .map(this::toReportView).toList();
    }

    public long countLecturerReports(String username) {
        return reportRepository.countByRegistrationThesisSupervisorUserAccountUsername(username);
    }

    private ThesisView toThesisView(Thesis thesis) {
        return new ThesisView(thesis.getId(), thesis.getTitle(), thesis.getDescription(), thesis.getAcademicYear(),
                thesis.getSemester(), thesis.getSupervisor().getFullName());
    }

    private List<RegistrationView> toRegistrationViews(List<Registration> registrations) {
        if (registrations.isEmpty()) {
            return List.of();
        }
        Map<Long, Report> reportsByRegistrationId = reportRepository
                .findByRegistrationIdIn(registrations.stream().map(Registration::getId).toList()).stream()
                .collect(Collectors.toMap(report -> report.getRegistration().getId(), Function.identity()));
        return registrations.stream().map(registration -> {
            Report report = reportsByRegistrationId.get(registration.getId());
            ReportStatus reportStatus = report == null ? null : report.getStatus();
            return new RegistrationView(registration.getId(), registration.getThesis().getId(),
                    registration.getThesis().getTitle(), registration.getStudent().getFullName(),
                    registration.getStatus(), reportStatus);
        }).toList();
    }

    private ReportView toReportView(Report report) {
        String submission = report.getReportFile();
        if (submission != null && !submission.startsWith("reports/")) {
            submission = externalUrlOrNull(submission);
        }
        return new ReportView(report.getId(), report.getRegistration().getThesis().getTitle(),
                report.getRegistration().getStudent().getFullName(), submission, report.getStatus(),
                report.getFeedback());
    }

    private String externalUrlOrNull(String value) {
        return ExternalReportUrl.normalizeOrNull(value);
    }
}
