package com.glucoze.thesismanagement.home.service;

import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.enums.ReportStatus;
import com.glucoze.thesismanagement.council.dto.ScheduleView;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.council.repository.CouncilRepository;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.grading.entity.Result;
import com.glucoze.thesismanagement.grading.repository.ResultRepository;
import com.glucoze.thesismanagement.home.dto.AdminDashboardView;
import com.glucoze.thesismanagement.home.dto.LecturerDashboardView;
import com.glucoze.thesismanagement.home.dto.StudentDashboardView;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.entity.Report;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ReportRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardQueryService {
    private static final PageRequest UPCOMING_LIMIT = PageRequest.of(0, 5);

    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final ThesisRepository thesisRepository;
    private final RegistrationRepository registrationRepository;
    private final ReportRepository reportRepository;
    private final CouncilRepository councilRepository;
    private final DefenseScheduleRepository scheduleRepository;
    private final ResultRepository resultRepository;
    private final Clock clock;

    @Autowired
    public DashboardQueryService(StudentRepository studentRepository, LecturerRepository lecturerRepository,
                                 ThesisRepository thesisRepository, RegistrationRepository registrationRepository,
                                 ReportRepository reportRepository, CouncilRepository councilRepository,
                                 DefenseScheduleRepository scheduleRepository, ResultRepository resultRepository) {
        this(studentRepository, lecturerRepository, thesisRepository, registrationRepository, reportRepository,
                councilRepository, scheduleRepository, resultRepository, Clock.systemDefaultZone());
    }

    DashboardQueryService(StudentRepository studentRepository, LecturerRepository lecturerRepository,
                          ThesisRepository thesisRepository, RegistrationRepository registrationRepository,
                          ReportRepository reportRepository, CouncilRepository councilRepository,
                          DefenseScheduleRepository scheduleRepository, ResultRepository resultRepository, Clock clock) {
        this.studentRepository = studentRepository;
        this.lecturerRepository = lecturerRepository;
        this.thesisRepository = thesisRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
        this.councilRepository = councilRepository;
        this.scheduleRepository = scheduleRepository;
        this.resultRepository = resultRepository;
        this.clock = clock;
    }

    public AdminDashboardView adminDashboard() {
        LocalDateTime now = LocalDateTime.now(clock);
        return new AdminDashboardView(studentRepository.count(), lecturerRepository.count(), thesisRepository.count(),
                registrationRepository.countByStatus(RegistrationStatus.PENDING),
                reportRepository.countByStatus(ReportStatus.SUBMITTED), councilRepository.count(),
                scheduleRepository.count(), resultRepository.countByPublishedTrue(),
                scheduleRepository.findUpcoming(now, UPCOMING_LIMIT).stream().map(this::toScheduleView).toList());
    }

    public LecturerDashboardView lecturerDashboard(String username) {
        LocalDateTime now = LocalDateTime.now(clock);
        return new LecturerDashboardView(thesisRepository.countBySupervisorUserAccountUsername(username),
                registrationRepository.countByThesisSupervisorUserAccountUsernameAndStatus(username, RegistrationStatus.PENDING),
                reportRepository.countByRegistrationThesisSupervisorUserAccountUsernameAndStatus(username, ReportStatus.SUBMITTED),
                scheduleRepository.countIncompleteSupervisorTasks(username) + scheduleRepository.countIncompleteCouncilTasks(username),
                scheduleRepository.findUpcomingByLecturerAccess(username, now, UPCOMING_LIMIT).stream()
                        .map(this::toScheduleView).toList());
    }

    public StudentDashboardView studentDashboard(String username) {
        Registration registration = registrationRepository
                .findByStudentUserAccountUsernameAndActiveRegistrationKeyTrue(username)
                .orElseGet(() -> registrationRepository
                        .findFirstByStudentUserAccountUsernameOrderByCreatedAtDesc(username).orElse(null));
        if (registration == null) return new StudentDashboardView(null, null, null, null, null, null, false, null);
        Report report = reportRepository.findByRegistrationId(registration.getId()).orElse(null);
        DefenseSchedule schedule = scheduleRepository.findByThesisId(registration.getThesis().getId()).orElse(null);
        Result result = schedule == null ? null : resultRepository.findByDefenseScheduleIdAndPublishedTrue(schedule.getId()).orElse(null);
        return new StudentDashboardView(registration.getThesis().getTitle(), registration.getStatus(),
                report == null ? null : report.getStatus(), schedule == null ? null : schedule.getStartTime(),
                schedule == null ? null : schedule.getRoom(), schedule == null ? null : schedule.getCouncil().getName(),
                result != null, result == null ? null : result.getFinalScore());
    }

    private ScheduleView toScheduleView(DefenseSchedule schedule) {
        return new ScheduleView(schedule.getId(), schedule.getThesis().getTitle(), schedule.getCouncil().getName(),
                schedule.getRoom(), schedule.getStartTime(), schedule.getEndTime());
    }
}
