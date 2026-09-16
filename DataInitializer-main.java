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
    private final PasswordEncoder passwordEncoder;
    private final GradingCalculator gradingCalculator;
    private final GradingService gradingService;

    public DataInitializer(UserAccountRepository userAccountRepository,
                          AdminRepository adminRepository,
                          LecturerRepository lecturerRepository,
                          StudentRepository studentRepository,
                          PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.adminRepository = adminRepository;
        this.lecturerRepository = lecturerRepository;
        this.studentRepository = studentRepository;
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
    void initializeDemoAccounts() {
        ensureDemoAccount("admin", Role.ADMIN, "Administrator", "AD001", null, null);
        ensureDemoAccount("lecturer01", Role.LECTURER, "Giảng viên 01", "GV001", "Khoa CNTT", null);
        ensureDemoAccount("student01", Role.STUDENT, "Sinh viên 01", "SV001", null, "K64-CNPM");
    }

    private void ensureDemoAccount(String username, Role role, String fullName,
                                   String code, String department, String className) {
        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseGet(() -> userAccountRepository.save(
                        new UserAccount(username, passwordEncoder.encode(DEMO_PASSWORD), role)));

        switch (role) {
            case ADMIN -> adminRepository.findByUserAccountUsername(username)
                    .orElseGet(() -> adminRepository.save(new Admin(account, fullName)));
            case LECTURER -> lecturerRepository.findByUserAccountUsername(username)
                    .orElseGet(() -> lecturerRepository.save(new Lecturer(account, fullName, code, department)));
            case STUDENT -> studentRepository.findByUserAccountUsername(username)
                    .orElseGet(() -> studentRepository.save(new Student(account, fullName, code, className)));
            default -> throw new IllegalArgumentException("Unsupported role for demo account: " + role);
        }
        return report;
    }

    private Map<String, Council> createCouncils(Map<String, Lecturer> lecturers) {
        Map<String, Council> councils = new LinkedHashMap<>();
        councils.put("software", council("Hội đồng Công nghệ phần mềm 01", "2026-2027", "1",
                lecturers.get("lecturer02"), lecturers.get("lecturer03"), lecturers.get("lecturer04")));
        councils.put("systems", council("Hội đồng Hệ thống thông tin 01", "2026-2027", "1",
                lecturers.get("lecturer04"), lecturers.get("lecturer05"), lecturers.get("lecturer06")));
        councils.put("computing", council("Hội đồng Khoa học máy tính 01", "2026-2027", "1",
                lecturers.get("lecturer01"), lecturers.get("lecturer05"), lecturers.get("lecturer06")));
        return councils;
    }

    private Council council(String name, String academicYear, String semester,
                            Lecturer chair, Lecturer secretary, Lecturer member) {
        Council council = councilRepository.save(new Council(name, academicYear, semester));
        memberRepository.saveAll(List.of(
                new CouncilMember(council, chair, CouncilRole.CHAIR),
                new CouncilMember(council, secretary, CouncilRole.SECRETARY),
                new CouncilMember(council, member, CouncilRole.MEMBER)));
        return council;
    }

    private Map<String, DefenseSchedule> createSchedules(Map<String, Thesis> theses,
                                                          Map<String, Council> councils) {
        Map<String, DefenseSchedule> schedules = new LinkedHashMap<>();
        schedules.put("ungraded", schedule(theses.get("ungraded"), councils.get("software"), "A101",
                LocalDateTime.of(2027, 6, 14, 8, 0)));
        schedules.put("partial", schedule(theses.get("partial"), councils.get("systems"), "A102",
                LocalDateTime.of(2027, 6, 14, 10, 0)));
        schedules.put("complete", schedule(theses.get("complete"), councils.get("computing"), "B201",
                LocalDateTime.of(2027, 6, 15, 8, 0)));
        schedules.put("published", schedule(theses.get("published"), councils.get("software"), "B202",
                LocalDateTime.of(2027, 6, 15, 10, 0)));
        return schedules;
    }

    private DefenseSchedule schedule(Thesis thesis, Council council, String room, LocalDateTime start) {
        return scheduleRepository.save(new DefenseSchedule(thesis, council, room, start, start.plusMinutes(60)));
    }

    private void createGradesAndPublishedResult(Map<String, DefenseSchedule> schedules,
                                                 Map<String, Lecturer> lecturers) {
        DefenseSchedule partial = schedules.get("partial");
        gradeRepository.saveAll(List.of(
                grade(partial, lecturers.get("lecturer02"), ScoreSource.SUPERVISOR, "8.0"),
                grade(partial, lecturers.get("lecturer04"), ScoreSource.COUNCIL, "7.5")));

        DefenseSchedule complete = schedules.get("complete");
        gradeRepository.saveAll(List.of(
                grade(complete, lecturers.get("lecturer03"), ScoreSource.SUPERVISOR, "8.5"),
                grade(complete, lecturers.get("lecturer01"), ScoreSource.COUNCIL, "8.0"),
                grade(complete, lecturers.get("lecturer05"), ScoreSource.COUNCIL, "8.5"),
                grade(complete, lecturers.get("lecturer06"), ScoreSource.COUNCIL, "9.0")));

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

    private void logDemoAccounts() {
        log.info("Dữ liệu demo đã sẵn sàng: admin/{}; lecturer01..06/{}; student01..06/{}",
                ADMIN_PASSWORD, LECTURER_PASSWORD, STUDENT_PASSWORD);
    }
}
