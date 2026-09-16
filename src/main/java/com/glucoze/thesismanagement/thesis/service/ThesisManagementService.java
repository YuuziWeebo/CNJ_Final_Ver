package com.glucoze.thesismanagement.thesis.service;

import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.common.enums.ReportStatus;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.thesis.dto.ReportForm;
import com.glucoze.thesismanagement.thesis.dto.ReviewForm;
import com.glucoze.thesismanagement.thesis.dto.ThesisForm;
import com.glucoze.thesismanagement.thesis.entity.Report;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.thesis.repository.ReportRepository;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.thesis.validation.ExternalReportUrl;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.Student;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.audit.AuditAction;
import com.glucoze.thesismanagement.audit.AuditEntityType;
import com.glucoze.thesismanagement.audit.AuditLogService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class ThesisManagementService {

    private final ThesisRepository thesisRepository;
    private final RegistrationRepository registrationRepository;
    private final ReportRepository reportRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final ReportFileStorage reportFileStorage;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public ThesisManagementService(ThesisRepository thesisRepository,
                                   RegistrationRepository registrationRepository,
                                   ReportRepository reportRepository,
                                   LecturerRepository lecturerRepository,
                                   StudentRepository studentRepository,
                                   ReportFileStorage reportFileStorage,
                                   NotificationService notificationService,
                                   AuditLogService auditLogService) {
        this.thesisRepository = thesisRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
        this.lecturerRepository = lecturerRepository;
        this.studentRepository = studentRepository;
        this.reportFileStorage = reportFileStorage;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public void createThesis(String username, ThesisForm form) {
        Lecturer lecturer = findLecturer(username);
        Thesis thesis = thesisRepository.save(new Thesis(clean(form.getTitle()), clean(form.getDescription()),
                clean(form.getAcademicYear()), clean(form.getSemester()), lecturer));
        auditLogService.append(AuditAction.THESIS_CREATE, AuditEntityType.THESIS, thesis.getId(),
                "Đã tạo đề tài “" + thesis.getTitle() + "”");
    }

    @Transactional
    public void updateThesis(String username, Long thesisId, ThesisForm form) {
        Thesis thesis = findLecturerThesis(username, thesisId);
        thesis.updateDetails(clean(form.getTitle()), clean(form.getDescription()),
                clean(form.getAcademicYear()), clean(form.getSemester()));
        auditLogService.append(AuditAction.THESIS_UPDATE, AuditEntityType.THESIS, thesis.getId(),
                "Đã cập nhật đề tài “" + thesis.getTitle() + "”");
    }

    @Transactional
    public void deleteThesis(String username, Long thesisId) {
        Thesis thesis = findLecturerThesis(username, thesisId);
        if (registrationRepository.existsByThesisId(thesis.getId())) {
            throw new DomainRuleViolationException("Không thể xóa đề tài đã có đăng ký");
        }
        thesisRepository.delete(thesis);
        auditLogService.append(AuditAction.THESIS_DELETE, AuditEntityType.THESIS, thesis.getId(),
                "Đã xóa đề tài “" + thesis.getTitle() + "”");
    }

    @Transactional
    public void registerForThesis(String username, Long thesisId) {
        Student student = studentRepository.findByUsernameForUpdate(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ sinh viên"));
        Thesis thesis = thesisRepository.findByIdForUpdate(thesisId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề tài"));
        registrationRepository.findByStudentIdAndActiveRegistrationKeyTrue(student.getId()).ifPresent(existing -> {
            if (existing.getThesis() == thesis
                    || (existing.getThesis().getId() != null
                    && existing.getThesis().getId().equals(thesis.getId()))) {
                throw new DomainRuleViolationException("Bạn đã đăng ký đề tài này");
            }
            if (existing.getStatus() != RegistrationStatus.PENDING) {
                throw new DomainRuleViolationException(
                        "Không thể tự đổi đề tài sau khi đăng ký đã được duyệt");
            }
            existing.cancelForChange();
        });
        if (registrationRepository.existsByThesisIdAndActiveRegistrationKeyTrue(thesis.getId())) {
            throw new DomainRuleViolationException("Đề tài đã có đăng ký đang hoạt động (already has an active registration)");
        }
        try {
            Registration registration = new Registration(thesis, student);
            registrationRepository.saveAndFlush(registration);
            notificationService.registrationSubmitted(registration);
        } catch (DataIntegrityViolationException exception) {
            throw new DomainRuleViolationException(
                    "Sinh viên hoặc đề tài đã có đăng ký đang hoạt động", exception);
        }
    }

    @Transactional
    public void approveRegistration(String username, Long registrationId) {
        Registration registration = findLecturerRegistration(username, registrationId);
        registration.transitionTo(RegistrationStatus.APPROVED);
        notificationService.registrationReviewed(registration, true);
        auditLogService.append(AuditAction.REGISTRATION_APPROVE, AuditEntityType.REGISTRATION, registration.getId(),
                "Đã duyệt đăng ký đề tài");
    }

    @Transactional
    public void rejectRegistration(String username, Long registrationId) {
        Registration registration = findLecturerRegistration(username, registrationId);
        registration.transitionTo(RegistrationStatus.REJECTED);
        notificationService.registrationReviewed(registration, false);
        auditLogService.append(AuditAction.REGISTRATION_REJECT, AuditEntityType.REGISTRATION, registration.getId(),
                "Đã từ chối đăng ký đề tài");
    }

    @Transactional
    public void submitReport(String username, Long registrationId, ReportForm form) {
        submitReport(username, registrationId, form, null);
    }

    @Transactional
    public void submitReport(String username, Long registrationId, ReportForm form, MultipartFile file) {
        Registration registration = findStudentRegistration(username, registrationId);
        requireApproved(registration);
        Report report = reportRepository.findByRegistrationId(registration.getId()).orElseGet(() -> new Report(registration));
        ReportStatus previousStatus = report.getStatus();
        if (previousStatus != ReportStatus.DRAFT && previousStatus != ReportStatus.REVISION_REQUIRED) {
            throw new DomainRuleViolationException("Báo cáo này không thể nộp nữa");
        }
        boolean hasFile = file != null && !file.isEmpty();
        String url = cleanNullable(form.getReportFile());
        if (!hasFile && (url == null || url.isBlank())) {
            throw new DomainRuleViolationException("Vui lòng đính kèm file hoặc cung cấp liên kết báo cáo.");
        }
        if (hasFile) {
            String previousPath = report.getReportFile();
            String relativePath = reportFileStorage.store(file);
            registerReportFileLifecycle(relativePath, previousPath);
            report.updateSubmission(relativePath);
        } else {
            String previousPath = report.getReportFile();
            report.updateSubmission(ExternalReportUrl.normalize(url));
            registerOldReportFileCleanup(previousPath);
        }
        report.transitionTo(ReportStatus.SUBMITTED);
        reportRepository.save(report);
        notificationService.reportSubmitted(report, previousStatus);
    }

    private void registerReportFileLifecycle(String newPath, String previousPath) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            reportFileStorage.delete(newPath);
            throw new IllegalStateException("Không thể lưu báo cáo ngoài transaction đang hoạt động");
        }
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    if (isStoredReportPath(previousPath) && !previousPath.equals(newPath)) {
                        reportFileStorage.delete(previousPath);
                    }
                }

                @Override
                public void afterCompletion(int status) {
                    if (status != TransactionSynchronization.STATUS_COMMITTED) {
                        reportFileStorage.delete(newPath);
                    }
                }
            });
        } catch (RuntimeException exception) {
            reportFileStorage.delete(newPath);
            throw exception;
        }
    }

    private void registerOldReportFileCleanup(String previousPath) {
        if (!isStoredReportPath(previousPath)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Không thể thay báo cáo ngoài transaction đang hoạt động");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                reportFileStorage.delete(previousPath);
            }
        });
    }

    private boolean isStoredReportPath(String path) {
        return path != null && path.startsWith("reports/");
    }

    /** Authorize and load an attached report owned by the current student. */
    @Transactional(readOnly = true)
    public ReportFileStorage.StoredReportFile getStudentReportFile(String username, Long reportId) {
        Report report = reportRepository.findByIdAndRegistrationStudentUserAccountUsername(reportId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo"));
        return loadAttachedReport(report);
    }

    /** Authorize and load an attached report supervised by the current lecturer. */
    @Transactional(readOnly = true)
    public ReportFileStorage.StoredReportFile getLecturerReportFile(String username, Long reportId) {
        Report report = reportRepository.findByIdAndRegistrationThesisSupervisorUserAccountUsername(reportId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo"));
        return loadAttachedReport(report);
    }

    private ReportFileStorage.StoredReportFile loadAttachedReport(Report report) {
        String path = report.getReportFile();
        if (path == null || path.isBlank() || !path.startsWith("reports/")) {
            throw new ResourceNotFoundException("Báo cáo không có file đính kèm");
        }
        return reportFileStorage.load(path);
    }


    @Transactional
    public void requestReportChanges(String username, Long reportId, ReviewForm form) {
        Report report = findLecturerReport(username, reportId);
        report.addReview(cleanNullable(form.getFeedback()));
        report.transitionTo(ReportStatus.REVISION_REQUIRED);
        notificationService.reportReviewed(report, false);
        auditLogService.append(AuditAction.REPORT_REVISION_REQUEST, AuditEntityType.REPORT, report.getId(),
                "Đã yêu cầu chỉnh sửa báo cáo");
    }

    @Transactional
    public void approveReport(String username, Long reportId, ReviewForm form) {
        Report report = findLecturerReport(username, reportId);
        report.addReview(cleanNullable(form.getFeedback()));
        report.transitionTo(ReportStatus.APPROVED);
        notificationService.reportReviewed(report, true);
        auditLogService.append(AuditAction.REPORT_APPROVE, AuditEntityType.REPORT, report.getId(),
                "Đã duyệt báo cáo");
    }

    private Lecturer findLecturer(String username) {
        return lecturerRepository.findByUserAccountUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ giảng viên"));
    }

    private Thesis findLecturerThesis(String username, Long thesisId) {
        return thesisRepository.findByIdAndSupervisorUserAccountUsername(thesisId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đề tài"));
    }

    private Registration findStudentRegistration(String username, Long registrationId) {
        return registrationRepository.findByIdAndStudentUserAccountUsername(registrationId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký"));
    }

    private Registration findLecturerRegistration(String username, Long registrationId) {
        return registrationRepository.findByIdAndThesisSupervisorUserAccountUsername(registrationId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký"));
    }

    private Report findLecturerReport(String username, Long reportId) {
        return reportRepository.findByIdAndRegistrationThesisSupervisorUserAccountUsername(reportId, username)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy báo cáo"));
    }

    private void requireApproved(Registration registration) {
        if (registration.getStatus() != RegistrationStatus.APPROVED) {
            throw new DomainRuleViolationException("Chỉ đăng ký đã duyệt mới được nộp báo cáo (approved registration required)");
        }
    }

    private String clean(String value) {
        return value.trim();
    }

    private String cleanNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
