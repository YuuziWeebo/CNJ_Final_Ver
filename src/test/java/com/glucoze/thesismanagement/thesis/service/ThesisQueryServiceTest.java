package com.glucoze.thesismanagement.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.thesis.entity.Report;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.thesis.repository.ReportRepository;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ThesisQueryServiceTest {

    @Test
    void lecturerListKeepsReadModelAndUsesOnlyQueryBoundary() {
        ThesisRepository theses = mock(ThesisRepository.class);
        RegistrationRepository registrations = mock(RegistrationRepository.class);
        ReportRepository reports = mock(ReportRepository.class);
        ThesisQueryService service = new ThesisQueryService(theses, registrations, reports);
        Lecturer lecturer = new Lecturer(
                new UserAccount("lecturer", "hash", Role.LECTURER), "Nguyễn Văn A", "GV01", "CNTT");
        when(theses.findBySupervisorUserAccountUsername("lecturer"))
                .thenReturn(List.of(new Thesis("Đề tài", "Mô tả", "2025-2026", "1", lecturer)));

        var result = service.listLecturerTheses("lecturer");

        assertThat(result).singleElement().satisfies(view -> {
            assertThat(view.title()).isEqualTo("Đề tài");
            assertThat(view.supervisor()).isEqualTo("Nguyễn Văn A");
        });
        verify(theses).findBySupervisorUserAccountUsername("lecturer");
    }
    @Test
    void reportFormKeepsStudentOwnershipAuthorization() {
        ThesisRepository theses = mock(ThesisRepository.class);
        RegistrationRepository registrations = mock(RegistrationRepository.class);
        ReportRepository reports = mock(ReportRepository.class);
        ThesisQueryService service = new ThesisQueryService(theses, registrations, reports);
        when(registrations.findByIdAndStudentUserAccountUsername(3L, "student"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getReportForm("student", 3L))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void existingHttpsReportLinkRemainsAvailableInForm() {
        ThesisRepository theses = mock(ThesisRepository.class);
        RegistrationRepository registrations = mock(RegistrationRepository.class);
        ReportRepository reports = mock(ReportRepository.class);
        ThesisQueryService service = new ThesisQueryService(theses, registrations, reports);
        Registration registration = mock(Registration.class);
        Report report = mock(Report.class);
        when(registration.getId()).thenReturn(3L);
        when(registration.getStatus()).thenReturn(RegistrationStatus.APPROVED);
        when(report.getReportFile()).thenReturn("https://example.com/old-report.pdf");
        when(registrations.findByIdAndStudentUserAccountUsername(3L, "student"))
                .thenReturn(Optional.of(registration));
        when(reports.findByRegistrationId(3L)).thenReturn(Optional.of(report));

        assertThat(service.getReportForm("student", 3L).getReportFile())
                .isEqualTo("https://example.com/old-report.pdf");
    }

}
