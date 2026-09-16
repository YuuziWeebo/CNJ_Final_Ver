package com.glucoze.thesismanagement.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.enums.ReportStatus;
import com.glucoze.thesismanagement.council.entity.Council;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.council.repository.CouncilRepository;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.grading.repository.ResultRepository;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ReportRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.Student;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class DashboardQueryServiceTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-11T08:00:00Z"), ZoneOffset.UTC);
    @Mock StudentRepository students; @Mock LecturerRepository lecturers; @Mock ThesisRepository theses;
    @Mock RegistrationRepository registrations; @Mock ReportRepository reports; @Mock CouncilRepository councils;
    @Mock DefenseScheduleRepository schedules; @Mock ResultRepository results;
    DashboardQueryService service;

    @BeforeEach void setUp() {
        service = new DashboardQueryService(students, lecturers, theses, registrations, reports, councils, schedules, results, CLOCK);
    }

    @Test void adminUsesCountsAndBoundedUpcomingQuery() {
        when(students.count()).thenReturn(20L); when(lecturers.count()).thenReturn(6L);
        when(registrations.countByStatus(RegistrationStatus.PENDING)).thenReturn(3L);
        when(reports.countByStatus(ReportStatus.SUBMITTED)).thenReturn(2L);
        when(schedules.findUpcoming(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        var view = service.adminDashboard();
        assertThat(view.studentCount()).isEqualTo(20); assertThat(view.lecturerCount()).isEqualTo(6);
        assertThat(view.pendingRegistrationCount()).isEqualTo(3); assertThat(view.pendingReportCount()).isEqualTo(2);
        verify(schedules).findUpcoming(LocalDateTime.of(2026, 9, 11, 8, 0), Pageable.ofSize(5));
        verify(students, never()).findAll();
    }

    @Test void lecturerQueriesAreAlwaysScopedByUsername() {
        when(schedules.findUpcomingByLecturerAccess(org.mockito.ArgumentMatchers.eq("gv"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        service.lecturerDashboard("gv");
        verify(theses).countBySupervisorUserAccountUsername("gv");
        verify(registrations).countByThesisSupervisorUserAccountUsernameAndStatus("gv", RegistrationStatus.PENDING);
        verify(reports).countByRegistrationThesisSupervisorUserAccountUsernameAndStatus("gv", ReportStatus.SUBMITTED);
        verify(schedules).countIncompleteSupervisorTasks("gv"); verify(schedules).countIncompleteCouncilTasks("gv");
    }

    @Test void studentNeverLoadsAnUnpublishedResult() {
        Lecturer supervisor = org.mockito.Mockito.mock(Lecturer.class);
        Student student = org.mockito.Mockito.mock(Student.class);
        Thesis thesis = new Thesis("Đề tài", "Mô tả", "2026-2027", "HK1", supervisor);
        Registration registration = new Registration(thesis, student);
        registration.transitionTo(RegistrationStatus.APPROVED);
        Council council = new Council("Hội đồng", "2026-2027", "HK1");
        DefenseSchedule schedule = new DefenseSchedule(thesis, council, "P101", LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK).plusHours(1));
        when(registrations.findByStudentUserAccountUsernameAndActiveRegistrationKeyTrue("sv")).thenReturn(Optional.of(registration));
        when(schedules.findByThesisId(thesis.getId())).thenReturn(Optional.of(schedule));
        when(results.findByDefenseScheduleIdAndPublishedTrue(schedule.getId())).thenReturn(Optional.empty());
        var view = service.studentDashboard("sv");
        assertThat(view.resultPublished()).isFalse(); assertThat(view.finalScore()).isNull();
        verify(results, never()).findByDefenseScheduleId(schedule.getId());
    }
}
