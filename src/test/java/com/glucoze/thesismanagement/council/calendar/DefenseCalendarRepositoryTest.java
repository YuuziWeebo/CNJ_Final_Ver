package com.glucoze.thesismanagement.council.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import com.glucoze.thesismanagement.common.enums.CouncilRole;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.config.JpaAuditingConfig;
import com.glucoze.thesismanagement.council.entity.Council;
import com.glucoze.thesismanagement.council.entity.CouncilMember;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.Student;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class DefenseCalendarRepositoryTest {
    @Autowired EntityManager entityManager;
    @Autowired DefenseScheduleRepository repository;
    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 10, 1, 0, 0);

    @Test void rangeAndRoleQueriesAreOrderedScopedAndDuplicateFree() {
        Lecturer supervisor = lecturer("supervisor", "GV01");
        Lecturer outsider = lecturer("outsider", "GV02");
        Lecturer memberOnly = lecturer("member-only", "GV03");
        Student owner = student("owner", "SV01");
        Student other = student("other", "SV02");
        Student secondOwner = student("second-owner", "SV03");
        Student thirdOwner = student("third-owner", "SV04");
        Council council = persist(new Council("Hội đồng", "2026-2027", "HK1"));
        persist(new CouncilMember(council, supervisor, CouncilRole.CHAIR));
        Council secondCouncil = persist(new Council("Hội đồng 2", "2026-2027", "HK1"));
        persist(new CouncilMember(secondCouncil, memberOnly, CouncilRole.SECRETARY));
        Thesis overlapThesis = thesis("Overlap", supervisor, owner);
        Thesis insideThesis = thesis("Inside", supervisor, secondOwner);
        Thesis outsideThesis = thesis("Outside", outsider, other);
        Thesis memberThesis = thesis("Member only", outsider, thirdOwner);
        persist(new DefenseSchedule(overlapThesis, council, "P100", START.minusHours(1), START.plusHours(1)));
        persist(new DefenseSchedule(insideThesis, council, "P101", START.plusDays(10), START.plusDays(10).plusHours(1)));
        persist(new DefenseSchedule(outsideThesis, council, "P102", END, END.plusHours(1)));
        persist(new DefenseSchedule(memberThesis, secondCouncil, "P103", START.plusDays(20), START.plusDays(20).plusHours(1)));
        entityManager.flush(); entityManager.clear();

        assertThat(repository.findCalendarRows(START, END)).extracting(row -> row.thesisTitle())
                .containsExactly("Overlap", "Inside", "Member only");
        assertThat(repository.findLecturerCalendarRows("supervisor", START, END)).extracting(row -> row.id())
                .doesNotHaveDuplicates().hasSize(2);
        assertThat(repository.findLecturerCalendarRows("member-only", START, END)).hasSize(1);
        assertThat(repository.findLecturerCalendarRows("outsider", START, END)).hasSize(1);
        assertThat(repository.findStudentCalendarRows("owner", RegistrationStatus.APPROVED, START, END)).hasSize(1);
        assertThat(repository.findStudentCalendarRows("other", RegistrationStatus.APPROVED, START, END)).isEmpty();
    }

    private Thesis thesis(String title, Lecturer supervisor, Student student) {
        Thesis thesis = persist(new Thesis(title, "Mô tả", "2026-2027", "HK1", supervisor));
        Registration registration = new Registration(thesis, student);
        registration.transitionTo(RegistrationStatus.APPROVED);
        persist(registration);
        return thesis;
    }

    private Lecturer lecturer(String username, String code) {
        UserAccount account = persist(new UserAccount(username, "hash", Role.LECTURER));
        return persist(new Lecturer(account, username, code, null));
    }

    private Student student(String username, String code) {
        UserAccount account = persist(new UserAccount(username, "hash", Role.STUDENT));
        return persist(new Student(account, username, code, null));
    }

    private <T> T persist(T entity) { entityManager.persist(entity); return entity; }
 }
