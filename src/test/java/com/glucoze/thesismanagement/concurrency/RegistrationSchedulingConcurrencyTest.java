package com.glucoze.thesismanagement.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import com.glucoze.thesismanagement.audit.AuditLogService;
import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.enums.CouncilRole;
import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.config.JpaAuditingConfig;
import com.glucoze.thesismanagement.council.dto.ScheduleForm;
import com.glucoze.thesismanagement.council.entity.Council;
import com.glucoze.thesismanagement.council.entity.CouncilMember;
import com.glucoze.thesismanagement.council.repository.CouncilRepository;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.council.service.CouncilSchedulingService;
import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.thesis.service.ReportFileStorage;
import com.glucoze.thesismanagement.thesis.service.ThesisManagementService;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.Student;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import({JpaAuditingConfig.class, ThesisManagementService.class, CouncilSchedulingService.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RegistrationSchedulingConcurrencyTest {

    @Autowired ThesisManagementService thesisService;
    @Autowired CouncilSchedulingService schedulingService;
    @Autowired UserAccountRepository accountRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired LecturerRepository lecturerRepository;
    @Autowired ThesisRepository thesisRepository;
    @Autowired RegistrationRepository registrationRepository;
    @Autowired CouncilRepository councilRepository;
    @Autowired CouncilMemberRepository memberRepository;
    @Autowired DefenseScheduleRepository scheduleRepository;
    @MockBean ReportFileStorage reportFileStorage;
    @MockBean NotificationService notificationService;
    @MockBean AuditLogService auditLogService;

    @Test
    void concurrentStudentsCannotBothRegisterForTheSameThesis() throws Exception {
        Lecturer supervisor = lecturer("supervisor", "GV00");
        Thesis thesis = thesisRepository.save(new Thesis("Topic", "Description", "2026", "1", supervisor));
        student("student-a", "SV01");
        student("student-b", "SV02");

        List<Throwable> outcomes = runConcurrently(
                () -> thesisService.registerForThesis("student-a", thesis.getId()),
                () -> thesisService.registerForThesis("student-b", thesis.getId()));

        assertOneSuccessAndOneDomainConflict(outcomes);
        assertThat(registrationRepository.findAll().stream().filter(Registration::isActive)).hasSize(1);
    }

    @Test
    void concurrentRequestsCannotCreateTwoSchedulesForTheSameThesis() throws Exception {
        Lecturer supervisor = lecturer("supervisor", "GV00");
        Thesis thesis = approvedThesis("Topic", supervisor, "student-a", "SV01");
        Council firstCouncil = council("Council A", "A");
        Council secondCouncil = council("Council B", "B");

        List<Throwable> outcomes = runConcurrently(
                () -> schedulingService.createSchedule(schedule(thesis, firstCouncil, "A101")),
                () -> schedulingService.createSchedule(schedule(thesis, secondCouncil, "B202")));

        assertOneSuccessAndOneDomainConflict(outcomes);
        assertThat(scheduleRepository.findAll()).hasSize(1);
    }

    @Test
    void concurrentOverlappingRequestsCannotUseTheSameCouncil() throws Exception {
        Lecturer supervisor = lecturer("supervisor", "GV00");
        Thesis first = approvedThesis("Topic A", supervisor, "student-a", "SV01");
        Thesis second = approvedThesis("Topic B", supervisor, "student-b", "SV02");
        Council council = council("Council A", "A");

        List<Throwable> outcomes = runConcurrently(
                () -> schedulingService.createSchedule(schedule(first, council, "A101")),
                () -> schedulingService.createSchedule(schedule(second, council, "B202")));

        assertOneSuccessAndOneDomainConflict(outcomes);
        assertThat(scheduleRepository.findAll()).hasSize(1);
    }

    @Test
    void concurrentOverlappingRequestsCannotUseSameRoomWithOtherwiseUnrelatedResources() throws Exception {
        Lecturer firstSupervisor = lecturer("supervisor-a", "GV-A");
        Lecturer secondSupervisor = lecturer("supervisor-b", "GV-B");
        Thesis first = approvedThesis("Topic A", firstSupervisor, "student-a", "SV01");
        Thesis second = approvedThesis("Topic B", secondSupervisor, "student-b", "SV02");
        Council firstCouncil = council("Council A", "A");
        Council secondCouncil = council("Council B", "B");

        List<Throwable> outcomes = runConcurrently(
                () -> schedulingService.createSchedule(schedule(first, firstCouncil, "A101")),
                () -> schedulingService.createSchedule(schedule(second, secondCouncil, "A101")));

        assertOneSuccessAndOneDomainConflict(outcomes);
        assertThat(scheduleRepository.findAll()).hasSize(1);
    }

    private List<Throwable> runConcurrently(ThrowingRunnable first, ThrowingRunnable second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<Throwable> firstResult = executor.submit(() -> runAfterBarrier(first, ready, start));
            Future<Throwable> secondResult = executor.submit(() -> runAfterBarrier(second, ready, start));
            boolean bothReady = ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            assertThat(bothReady).isTrue();
            return List.of(firstResult.get(10, TimeUnit.SECONDS), secondResult.get(10, TimeUnit.SECONDS));
        }
    }

    private Throwable runAfterBarrier(ThrowingRunnable task, CountDownLatch ready, CountDownLatch start) {
        ready.countDown();
        try {
            start.await();
            task.run();
            return new Success();
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private void assertOneSuccessAndOneDomainConflict(List<Throwable> outcomes) {
        assertThat(outcomes.stream().filter(Success.class::isInstance)).hasSize(1);
        assertThat(outcomes.stream().filter(DomainRuleViolationException.class::isInstance)).hasSize(1);
    }

    private Student student(String username, String code) {
        UserAccount account = accountRepository.save(new UserAccount(username, "hash", Role.STUDENT));
        return studentRepository.save(new Student(account, username, code, "C1"));
    }

    private Lecturer lecturer(String username, String code) {
        UserAccount account = accountRepository.save(new UserAccount(username, "hash", Role.LECTURER));
        return lecturerRepository.save(new Lecturer(account, username, code, "IT"));
    }

    private Thesis approvedThesis(String title, Lecturer supervisor, String studentUsername, String studentCode) {
        Thesis thesis = thesisRepository.save(new Thesis(title, "Description", "2026", "1", supervisor));
        Registration registration = new Registration(thesis, student(studentUsername, studentCode));
        registration.transitionTo(RegistrationStatus.APPROVED);
        registrationRepository.save(registration);
        return thesis;
    }

    private Council council(String name, String codePrefix) {
        Council council = councilRepository.save(new Council(name, "2026", "1"));
        memberRepository.save(new CouncilMember(council, lecturer("chair-" + codePrefix, codePrefix + "1"), CouncilRole.CHAIR));
        memberRepository.save(new CouncilMember(council, lecturer("secretary-" + codePrefix, codePrefix + "2"), CouncilRole.SECRETARY));
        memberRepository.save(new CouncilMember(council, lecturer("member-" + codePrefix, codePrefix + "3"), CouncilRole.MEMBER));
        return council;
    }

    private ScheduleForm schedule(Thesis thesis, Council council, String room) {
        ScheduleForm form = new ScheduleForm();
        form.setThesisId(thesis.getId());
        form.setCouncilId(council.getId());
        form.setRoom(room);
        form.setStartTime(LocalDateTime.of(2026, 9, 20, 9, 0));
        form.setEndTime(LocalDateTime.of(2026, 9, 20, 10, 0));
        return form;
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run();
    }

    static final class Success extends RuntimeException {
    }
}
