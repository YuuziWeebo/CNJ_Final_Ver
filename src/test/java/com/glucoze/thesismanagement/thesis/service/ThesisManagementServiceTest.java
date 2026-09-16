package com.glucoze.thesismanagement.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.enums.ReportStatus;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.thesis.dto.ReportForm;
import com.glucoze.thesismanagement.thesis.entity.Report;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.thesis.repository.ReportRepository;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.user.entity.Student;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import java.util.Optional;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.audit.AuditLogService;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ThesisManagementServiceTest {

    @TempDir
    Path uploadDir;

    @Mock
    private ThesisRepository thesisRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private LecturerRepository lecturerRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ReportFileStorage reportFileStorage;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;

    private ThesisManagementService service;

    @BeforeEach
    void setUp() {
        service = new ThesisManagementService(thesisRepository, registrationRepository, reportRepository,
                lecturerRepository, studentRepository, reportFileStorage, notificationService, auditLogService);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void changesStudentToAnotherThesisWhenStudentHasPendingRegistration() {
        Student student = student();
        Registration existing = new Registration(thesis(), student);
        when(studentRepository.findByUsernameForUpdate("student01")).thenReturn(Optional.of(student));
        when(thesisRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(thesis()));
        when(registrationRepository.findByStudentIdAndActiveRegistrationKeyTrue(null)).thenReturn(Optional.of(existing));
        when(registrationRepository.existsByThesisIdAndActiveRegistrationKeyTrue(null)).thenReturn(false);

        service.registerForThesis("student01", 2L);

        assertThat(existing.isActive()).isFalse();
        verify(registrationRepository).saveAndFlush(any(Registration.class));
    }

    @Test
    void rejectsChangingThesisAfterRegistrationWasApproved() {
        Student student = student();
        Registration existing = new Registration(thesis(), student);
        existing.transitionTo(RegistrationStatus.APPROVED);
        when(studentRepository.findByUsernameForUpdate("student01")).thenReturn(Optional.of(student));
        when(thesisRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(thesis()));
        when(registrationRepository.findByStudentIdAndActiveRegistrationKeyTrue(null)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.registerForThesis("student01", 2L))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessage("Không thể tự đổi đề tài sau khi đăng ký đã được duyệt");

        assertThat(existing.getStatus()).isEqualTo(RegistrationStatus.APPROVED);
        assertThat(existing.isActive()).isTrue();
        verify(registrationRepository, never()).saveAndFlush(any(Registration.class));
        verifyNoInteractions(notificationService);
    }

    @Test
    void approvalNotifiesStudentOnlyAfterValidTransition() {
        Registration registration = new Registration(thesis(), student());
        when(registrationRepository.findByIdAndThesisSupervisorUserAccountUsername(7L, "lecturer01"))
                .thenReturn(Optional.of(registration));

        service.approveRegistration("lecturer01", 7L);

        verify(notificationService).registrationReviewed(registration, true);
    }

    @Test
    void repeatedApprovalDoesNotCreateDuplicateNotification() {
        Registration registration = new Registration(thesis(), student());
        registration.transitionTo(RegistrationStatus.APPROVED);
        when(registrationRepository.findByIdAndThesisSupervisorUserAccountUsername(7L, "lecturer01"))
                .thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> service.approveRegistration("lecturer01", 7L))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(notificationService);
    }

    @Test
    void rejectsRegistrationWhenThesisHasActiveRegistration() {
        Student student = student();
        Thesis thesis = thesis();
        when(studentRepository.findByUsernameForUpdate("student01")).thenReturn(Optional.of(student));
        when(thesisRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(thesis));
        when(registrationRepository.existsByThesisIdAndActiveRegistrationKeyTrue(null)).thenReturn(true);

        assertThatThrownBy(() -> service.registerForThesis("student01", 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has an active registration");
        verify(registrationRepository, never()).saveAndFlush(any(Registration.class));
    }

    @Test
    void mapsRegistrationUniqueConstraintRaceToDomainError() {
        Student student = student();
        Thesis thesis = thesis();
        when(studentRepository.findByUsernameForUpdate("student01")).thenReturn(Optional.of(student));
        when(thesisRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(thesis));
        when(registrationRepository.saveAndFlush(any(Registration.class)))
                .thenThrow(new DataIntegrityViolationException("SQL constraint detail"));

        assertThatThrownBy(() -> service.registerForThesis("student01", 2L))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessage("Sinh viên hoặc đề tài đã có đăng ký đang hoạt động")
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsReportSubmissionForUnapprovedRegistration() {
        Registration registration = new Registration(thesis(), student());
        when(registrationRepository.findByIdAndStudentUserAccountUsername(3L, "student01"))
                .thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> service.submitReport("student01", 3L, new ReportForm("report.pdf")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approved registration");
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void allowsResubmissionOnlyAfterRequestedChanges() {
        Registration registration = new Registration(thesis(), student());
        registration.transitionTo(RegistrationStatus.APPROVED);
        Report report = new Report(registration);
        report.transitionTo(ReportStatus.SUBMITTED);
        report.transitionTo(ReportStatus.REVISION_REQUIRED);
        when(registrationRepository.findByIdAndStudentUserAccountUsername(3L, "student01"))
                .thenReturn(Optional.of(registration));
        when(reportRepository.findByRegistrationId(null)).thenReturn(Optional.of(report));

        service.submitReport("student01", 3L, new ReportForm("https://example.com/revised-report.pdf"));

        assertThat(report.getStatus()).isEqualTo(ReportStatus.SUBMITTED);
        assertThat(report.getReportFile()).isEqualTo("https://example.com/revised-report.pdf");
        verify(reportRepository).save(report);
    }

    @Test
    void rejectsSubmissionWithoutFileOrExternalLink() {
        Registration registration = new Registration(thesis(), student());
        registration.transitionTo(RegistrationStatus.APPROVED);
        when(registrationRepository.findByIdAndStudentUserAccountUsername(3L, "student01"))
                .thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> service.submitReport("student01", 3L, new ReportForm()))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("đính kèm file hoặc cung cấp liên kết");
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void rejectsUnsafeExternalLinkWithoutPersistingIt() {
        Registration registration = new Registration(thesis(), student());
        registration.transitionTo(RegistrationStatus.APPROVED);
        when(registrationRepository.findByIdAndStudentUserAccountUsername(3L, "student01"))
                .thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> service.submitReport(
                "student01", 3L, new ReportForm("javascript:alert(1)")))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("HTTPS");
        verify(reportRepository, never()).save(any(Report.class));
    }

    @Test
    void lecturerCannotReviewReportOutsideTheirSupervision() {
        when(reportRepository.findByIdAndRegistrationThesisSupervisorUserAccountUsername(4L, "lecturer01"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveReport("lecturer01", 4L, new com.glucoze.thesismanagement.thesis.dto.ReviewForm()))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void studentCannotDownloadAnotherStudentsReport() {
        when(reportRepository.findByIdAndRegistrationStudentUserAccountUsername(4L, "student01"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStudentReportFile("student01", 4L))
                .isInstanceOf(java.util.NoSuchElementException.class);
        verify(reportFileStorage, never()).load(any());
    }

    @Test
    void lecturerCanOnlyDownloadReportsUnderTheirSupervision() {
        when(reportRepository.findByIdAndRegistrationThesisSupervisorUserAccountUsername(4L, "lecturer01"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLecturerReportFile("lecturer01", 4L))
                .isInstanceOf(java.util.NoSuchElementException.class);
        verify(reportFileStorage, never()).load(any());
    }

    @Test
    void databaseFailureAfterNewFileCreationCleansNewFileOnRollback() {
        ReportFileStorage storage = new ReportFileStorage(uploadDir.toString());
        ThesisManagementService transactionalService = serviceWith(storage);
        Report report = reportReadyForReplacement("reports/existing.pdf");
        stubReportSubmission(report);
        when(reportRepository.save(report)).thenThrow(new RuntimeException("injected database failure"));
        TransactionSynchronizationManager.initSynchronization();

        assertThatThrownBy(() -> transactionalService.submitReport(
                "student01", 3L, new ReportForm(), pdfUpload("replacement.pdf", "replacement")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("database failure");

        String newPath = report.getReportFile();
        assertThat(storage.load(newPath).content()).isNotEmpty();
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);
        assertThatThrownBy(() -> storage.load(newPath)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void successfulReplacementKeepsOldFileUntilCommitThenDeletesIt() {
        ReportFileStorage storage = new ReportFileStorage(uploadDir.toString());
        String oldPath = storage.store(pdfUpload("old.pdf", "old"));
        ThesisManagementService transactionalService = serviceWith(storage);
        Report report = reportReadyForReplacement(oldPath);
        stubReportSubmission(report);
        TransactionSynchronizationManager.initSynchronization();

        transactionalService.submitReport(
                "student01", 3L, new ReportForm("https://example.com/ignored"),
                pdfUpload("new.pdf", "new"));

        String newPath = report.getReportFile();
        assertThat(newPath).isNotEqualTo(oldPath);
        assertThat(storage.load(oldPath).content()).isNotEmpty();
        assertThat(storage.load(newPath).content()).isNotEmpty();
        verify(reportRepository).save(report);

        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        assertThatThrownBy(() -> storage.load(oldPath)).isInstanceOf(IllegalArgumentException.class);
        assertThat(storage.load(newPath).content()).isNotEmpty();
    }

    @Test
    void switchingFromFileToHttpsLinkDeletesOnlyOldFileAfterCommit() {
        ReportFileStorage storage = new ReportFileStorage(uploadDir.toString());
        String oldPath = storage.store(pdfUpload("old.pdf", "old"));
        ThesisManagementService transactionalService = serviceWith(storage);
        Report report = reportReadyForReplacement(oldPath);
        stubReportSubmission(report);
        TransactionSynchronizationManager.initSynchronization();

        transactionalService.submitReport(
                "student01", 3L, new ReportForm("https://example.com/report.pdf"));

        assertThat(report.getReportFile()).isEqualTo("https://example.com/report.pdf");
        assertThat(storage.load(oldPath).content()).isNotEmpty();
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);
        assertThatThrownBy(() -> storage.load(oldPath)).isInstanceOf(IllegalArgumentException.class);
    }

    private ThesisManagementService serviceWith(ReportFileStorage storage) {
        return new ThesisManagementService(thesisRepository, registrationRepository, reportRepository,
                lecturerRepository, studentRepository, storage, notificationService, auditLogService);
    }

    private Report reportReadyForReplacement(String path) {
        Registration registration = new Registration(thesis(), student());
        registration.transitionTo(RegistrationStatus.APPROVED);
        Report report = new Report(registration);
        report.updateSubmission(path);
        report.transitionTo(ReportStatus.SUBMITTED);
        report.transitionTo(ReportStatus.REVISION_REQUIRED);
        return report;
    }

    private void stubReportSubmission(Report report) {
        when(registrationRepository.findByIdAndStudentUserAccountUsername(3L, "student01"))
                .thenReturn(Optional.of(report.getRegistration()));
        when(reportRepository.findByRegistrationId(null)).thenReturn(Optional.of(report));
    }

    private MockMultipartFile pdfUpload(String filename, String body) {
        return new MockMultipartFile("file", filename, "application/pdf",
                ("%PDF-1.7\n" + body).getBytes(StandardCharsets.US_ASCII));
    }

    private void completeTransaction(int status) {
        var synchronizations = TransactionSynchronizationManager.getSynchronizations();
        if (status == TransactionSynchronization.STATUS_COMMITTED) {
            synchronizations.forEach(TransactionSynchronization::afterCommit);
        }
        synchronizations.forEach(synchronization -> synchronization.afterCompletion(status));
        TransactionSynchronizationManager.clearSynchronization();
    }

    private Student student() {
        return new Student(new UserAccount("student01", "hash", com.glucoze.thesismanagement.common.enums.Role.STUDENT),
                "Student", "SV01", "C1");
    }

    private Thesis thesis() {
        return new Thesis("Topic", "Description", "2025-2026", "1", null);
    }
}
