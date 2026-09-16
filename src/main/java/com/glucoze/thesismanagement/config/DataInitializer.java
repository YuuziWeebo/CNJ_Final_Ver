package com.glucoze.thesismanagement.config;

import com.glucoze.thesismanagement.common.enums.CouncilRole;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.enums.ReportStatus;
import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.common.enums.ScoreSource;
import com.glucoze.thesismanagement.council.entity.Council;
import com.glucoze.thesismanagement.council.entity.CouncilMember;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.council.repository.CouncilRepository;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.grading.entity.Grade;
import com.glucoze.thesismanagement.grading.repository.GradeRepository;
import com.glucoze.thesismanagement.grading.service.GradingCalculator;
import com.glucoze.thesismanagement.grading.service.GradingService;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.entity.Report;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ReportRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.user.entity.Admin;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.Student;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates a deterministic, development-only dataset for demonstrations. */
@Component
@Profile("dev")
public class DataInitializer implements ApplicationRunner {

    static final String ADMIN_PASSWORD = "Admin@123";
    static final String LECTURER_PASSWORD = "Lecturer@123";
    static final String STUDENT_PASSWORD = "Student@123";

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserAccountRepository accountRepository;
    private final AdminRepository adminRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final ThesisRepository thesisRepository;
    private final RegistrationRepository registrationRepository;
    private final ReportRepository reportRepository;
    private final CouncilRepository councilRepository;
    private final CouncilMemberRepository memberRepository;
    private final DefenseScheduleRepository scheduleRepository;
    private final GradeRepository gradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final GradingCalculator gradingCalculator;
    private final GradingService gradingService;

    public DataInitializer(UserAccountRepository accountRepository,
                           AdminRepository adminRepository,
                           LecturerRepository lecturerRepository,
                           StudentRepository studentRepository,
                           ThesisRepository thesisRepository,
                           RegistrationRepository registrationRepository,
                           ReportRepository reportRepository,
                           CouncilRepository councilRepository,
                           CouncilMemberRepository memberRepository,
                           DefenseScheduleRepository scheduleRepository,
                           GradeRepository gradeRepository,
                           PasswordEncoder passwordEncoder,
                           GradingCalculator gradingCalculator,
                           GradingService gradingService) {
        this.accountRepository = accountRepository;
        this.adminRepository = adminRepository;
        this.lecturerRepository = lecturerRepository;
        this.studentRepository = studentRepository;
        this.thesisRepository = thesisRepository;
        this.registrationRepository = registrationRepository;
        this.reportRepository = reportRepository;
        this.councilRepository = councilRepository;
        this.memberRepository = memberRepository;
        this.scheduleRepository = scheduleRepository;
        this.gradeRepository = gradeRepository;
        this.passwordEncoder = passwordEncoder;
        this.gradingCalculator = gradingCalculator;
        this.gradingService = gradingService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initializeDemoDataset();
    }

    @Transactional
    void initializeDemoDataset() {
        Map<String, Lecturer> lecturers = createAccountsAndProfiles();
        if (thesisRepository.count() > 0 || registrationRepository.count() > 0
                || councilRepository.count() > 0) {
            log.info("Dữ liệu nghiệp vụ đã tồn tại; bỏ qua bước tạo dữ liệu demo để tránh ghi đè.");
            logDemoAccounts();
            return;
        }

        Map<String, Student> students = loadStudents();
        Map<String, Thesis> theses = createTheses(lecturers);
        Map<String, Registration> registrations = createRegistrations(theses, students);
        createReports(registrations);
        Map<String, Council> councils = createCouncils(lecturers);
        Map<String, DefenseSchedule> schedules = createSchedules(theses, councils);
        createGradesAndPublishedResult(schedules, lecturers);
        logDemoAccounts();
    }

    private Map<String, Lecturer> createAccountsAndProfiles() {
        ensureAccount("admin", Role.ADMIN, ADMIN_PASSWORD);
        adminRepository.findByUserAccountUsername("admin").orElseGet(() -> adminRepository.save(
                new Admin(requireAccount("admin"), "Quản trị viên")));

        Map<String, Lecturer> lecturers = new LinkedHashMap<>();
        for (int i = 1; i <= 6; i++) {
            String username = numberedUsername("lecturer", i);
            ensureAccount(username, Role.LECTURER, LECTURER_PASSWORD);
            int number = i;
            lecturers.put(username, lecturerRepository.findByUserAccountUsername(username).orElseGet(() ->
                    lecturerRepository.save(new Lecturer(requireAccount(username), "Giảng viên " + twoDigits(number),
                            "GV" + threeDigits(number), "Khoa Công nghệ thông tin"))));
        }
        for (int i = 1; i <= 6; i++) {
            String username = numberedUsername("student", i);
            ensureAccount(username, Role.STUDENT, STUDENT_PASSWORD);
            int number = i;
            studentRepository.findByUserAccountUsername(username).orElseGet(() -> studentRepository.save(
                    new Student(requireAccount(username), "Sinh viên " + twoDigits(number),
                            "SV" + threeDigits(number), "K64-CNPM")));
        }
        return lecturers;
    }

    private void ensureAccount(String username, Role role, String password) {
        accountRepository.findByUsername(username).orElseGet(() -> accountRepository.save(
                new UserAccount(username, passwordEncoder.encode(password), role)));
    }

    private UserAccount requireAccount(String username) {
        return accountRepository.findByUsername(username).orElseThrow();
    }

    private Map<String, Student> loadStudents() {
        Map<String, Student> students = new LinkedHashMap<>();
        for (int i = 1; i <= 6; i++) {
            String username = numberedUsername("student", i);
            students.put(username, studentRepository.findByUserAccountUsername(username).orElseThrow());
        }
        return students;
    }

    private Map<String, Thesis> createTheses(Map<String, Lecturer> lecturers) {
        Map<String, Thesis> theses = new LinkedHashMap<>();
        theses.put("ungraded", thesis("Hệ thống quản lý thư viện thông minh", lecturers.get("lecturer01")));
        theses.put("partial", thesis("Nền tảng quản lý khóa luận", lecturers.get("lecturer02")));
        theses.put("complete", thesis("Phân tích dữ liệu học tập", lecturers.get("lecturer03")));
        theses.put("published", thesis("Ứng dụng hỗ trợ chăm sóc sức khỏe", lecturers.get("lecturer04")));
        theses.put("pending", thesis("Website thương mại điện tử", lecturers.get("lecturer05")));
        theses.put("rejected", thesis("Nhận diện hình ảnh bằng AI", lecturers.get("lecturer06")));
        theses.put("cancelled", thesis("Ứng dụng quản lý công việc", lecturers.get("lecturer01")));
        theses.put("available", thesis("Hệ thống đặt lịch trực tuyến", lecturers.get("lecturer02")));
        return theses;
    }

    private Thesis thesis(String title, Lecturer supervisor) {
        return thesisRepository.save(new Thesis(title, "Dữ liệu minh họa phục vụ môi trường phát triển.",
                "2026-2027", "1", supervisor));
    }

    private Map<String, Registration> createRegistrations(Map<String, Thesis> theses,
                                                           Map<String, Student> students) {
        Map<String, Registration> registrations = new LinkedHashMap<>();
        registrations.put("ungraded", registration(theses.get("ungraded"), students.get("student01"),
                RegistrationStatus.APPROVED));
        registrations.put("partial", registration(theses.get("partial"), students.get("student02"),
                RegistrationStatus.APPROVED));
        registrations.put("complete", registration(theses.get("complete"), students.get("student03"),
                RegistrationStatus.APPROVED));
        registrations.put("published", registration(theses.get("published"), students.get("student04"),
                RegistrationStatus.APPROVED));
        registrations.put("pending", registration(theses.get("pending"), students.get("student05"),
                RegistrationStatus.PENDING));
        registrations.put("rejected", registration(theses.get("rejected"), students.get("student06"),
                RegistrationStatus.REJECTED));
        registrations.put("cancelled", registration(theses.get("cancelled"), students.get("student06"),
                RegistrationStatus.CANCELLED));
        return registrations;
    }

    private Registration registration(Thesis thesis, Student student, RegistrationStatus status) {
        Registration registration = new Registration(thesis, student);
        if (status != RegistrationStatus.PENDING) {
            registration.transitionTo(status);
        }
        return registrationRepository.save(registration);
    }

    private void createReports(Map<String, Registration> registrations) {
        reportRepository.save(report(registrations.get("ungraded"), "demo/ungraded.pdf", ReportStatus.SUBMITTED));
        reportRepository.save(report(registrations.get("partial"), "demo/partial.pdf", ReportStatus.REVISION_REQUIRED));
        reportRepository.save(report(registrations.get("complete"), "demo/complete.pdf", ReportStatus.APPROVED));
        reportRepository.save(report(registrations.get("published"), "demo/published.pdf", ReportStatus.APPROVED));
    }

    private Report report(Registration registration, String file, ReportStatus status) {
        Report report = new Report(registration);
        report.updateSubmission(file);
        report.transitionTo(ReportStatus.SUBMITTED);
        if (status != ReportStatus.SUBMITTED) {
            report.transitionTo(status);
        }
        return report;
    }

    private Map<String, Council> createCouncils(Map<String, Lecturer> lecturers) {
        Map<String, Council> councils = new LinkedHashMap<>();
        councils.put("software", council("Hội đồng Công nghệ phần mềm 01",
                lecturers.get("lecturer02"), lecturers.get("lecturer03"), lecturers.get("lecturer04")));
        councils.put("systems", council("Hội đồng Hệ thống thông tin 01",
                lecturers.get("lecturer04"), lecturers.get("lecturer05"), lecturers.get("lecturer06")));
        councils.put("computing", council("Hội đồng Khoa học máy tính 01",
                lecturers.get("lecturer01"), lecturers.get("lecturer05"), lecturers.get("lecturer06")));
        return councils;
    }

    private Council council(String name, Lecturer chair, Lecturer secretary, Lecturer member) {
        Council council = councilRepository.save(new Council(name, "2026-2027", "1"));
        memberRepository.saveAll(List.of(new CouncilMember(council, chair, CouncilRole.CHAIR),
                new CouncilMember(council, secretary, CouncilRole.SECRETARY),
                new CouncilMember(council, member, CouncilRole.MEMBER)));
        return council;
    }

    private Map<String, DefenseSchedule> createSchedules(Map<String, Thesis> theses, Map<String, Council> councils) {
        Map<String, DefenseSchedule> schedules = new LinkedHashMap<>();
        schedules.put("ungraded", schedule(theses.get("ungraded"), councils.get("software"), "A101", 14, 8));
        schedules.put("partial", schedule(theses.get("partial"), councils.get("systems"), "A102", 14, 10));
        schedules.put("complete", schedule(theses.get("complete"), councils.get("computing"), "B201", 15, 8));
        schedules.put("published", schedule(theses.get("published"), councils.get("software"), "B202", 15, 10));
        return schedules;
    }

    private DefenseSchedule schedule(Thesis thesis, Council council, String room, int day, int hour) {
        LocalDateTime start = LocalDateTime.of(2027, 6, day, hour, 0);
        return scheduleRepository.save(new DefenseSchedule(thesis, council, room, start, start.plusHours(1)));
    }

    private void createGradesAndPublishedResult(Map<String, DefenseSchedule> schedules,
                                                 Map<String, Lecturer> lecturers) {
        gradeRepository.saveAll(List.of(
                grade(schedules.get("partial"), lecturers.get("lecturer02"), ScoreSource.SUPERVISOR, "8.0"),
                grade(schedules.get("partial"), lecturers.get("lecturer04"), ScoreSource.COUNCIL, "7.5"),
                grade(schedules.get("complete"), lecturers.get("lecturer03"), ScoreSource.SUPERVISOR, "8.5"),
                grade(schedules.get("complete"), lecturers.get("lecturer01"), ScoreSource.COUNCIL, "8.0"),
                grade(schedules.get("complete"), lecturers.get("lecturer05"), ScoreSource.COUNCIL, "8.5"),
                grade(schedules.get("complete"), lecturers.get("lecturer06"), ScoreSource.COUNCIL, "9.0")));

        DefenseSchedule published = schedules.get("published");
        gradeRepository.saveAllAndFlush(List.of(
                grade(published, lecturers.get("lecturer04"), ScoreSource.SUPERVISOR, "9.0"),
                grade(published, lecturers.get("lecturer02"), ScoreSource.COUNCIL, "8.5"),
                grade(published, lecturers.get("lecturer03"), ScoreSource.COUNCIL, "9.0"),
                grade(published, lecturers.get("lecturer04"), ScoreSource.COUNCIL, "8.0")));
        gradingService.publishResult("lecturer02", published.getId());
    }

    private Grade grade(DefenseSchedule schedule, Lecturer lecturer, ScoreSource source, String score) {
        BigDecimal value = new BigDecimal(score);
        gradingCalculator.validateScore(value, source.toString());
        return new Grade(schedule, lecturer, source, value);
    }

    private String numberedUsername(String prefix, int number) {
        return prefix + twoDigits(number);
    }

    private String twoDigits(int number) {
        return String.format("%02d", number);
    }

    private String threeDigits(int number) {
        return String.format("%03d", number);
    }

    private void logDemoAccounts() {
        log.info("Dữ liệu demo đã sẵn sàng: admin/{}; lecturer01..06/{}; student01..06/{}",
                ADMIN_PASSWORD, LECTURER_PASSWORD, STUDENT_PASSWORD);
    }
}
