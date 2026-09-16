package com.glucoze.thesismanagement.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.common.enums.CouncilRole;
import com.glucoze.thesismanagement.council.entity.Council;
import com.glucoze.thesismanagement.council.entity.CouncilMember;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.thesis.entity.Registration;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.Student;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
    @Mock NotificationRepository repository;

    @Test void unreadCountUsesDatabaseCount() {
        NotificationService service = new NotificationService(repository);
        service.unreadCount("owner");
        verify(repository).countByUserAccountUsernameAndReadFalse("owner");
    }

    @Test void listIsOwnerScopedNewestFirstAndPageSizeIsBounded() {
        when(repository.findByUserAccountUsername(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(org.springframework.data.domain.Page.empty());
        new NotificationService(repository).listForUser("owner", -20);
        verify(repository).findByUserAccountUsername("owner",
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test void ownerCanMarkNotificationRead() {
        Notification notification = new Notification(new UserAccount("owner", "hash", Role.STUDENT),
                NotificationType.REPORT_APPROVED, "Title", "Message", "/student/reports");
        when(repository.findByIdAndUserAccountUsername(1L, "owner")).thenReturn(Optional.of(notification));
        new NotificationService(repository).markRead("owner", 1L);
        org.assertj.core.api.Assertions.assertThat(notification.isRead()).isTrue();
    }

    @Test void wrongOwnerCannotMarkNotificationRead() {
        when(repository.findByIdAndUserAccountUsername(1L, "attacker")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new NotificationService(repository).markRead("attacker", 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void markAllIsScopedToOwner() {
        new NotificationService(repository).markAllRead("owner");
        verify(repository).markAllReadByUsername("owner");
    }

    @Test void scheduleRecipientsAreDeduplicatedWhenSupervisorIsCouncilMember() {
        UserAccount supervisorAccount = new UserAccount("supervisor", "hash", Role.LECTURER);
        Lecturer supervisor = new Lecturer(supervisorAccount, "Supervisor", "GV01", null);
        UserAccount memberAccount = new UserAccount("member", "hash", Role.LECTURER);
        Lecturer member = new Lecturer(memberAccount, "Member", "GV02", null);
        UserAccount studentAccount = new UserAccount("student", "hash", Role.STUDENT);
        Student student = new Student(studentAccount, "Student", "SV01", null);
        Thesis thesis = new Thesis("Topic", "Description", "2026-2027", "HK1", supervisor);
        Registration registration = new Registration(thesis, student);
        Council council = new Council("Council", "2026-2027", "HK1");
        DefenseSchedule schedule = new DefenseSchedule(thesis, council, "A1", LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        new NotificationService(repository).defenseScheduled(schedule, registration, List.of(
                new CouncilMember(council, supervisor, CouncilRole.CHAIR),
                new CouncilMember(council, member, CouncilRole.MEMBER)));

        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(5)).save(org.mockito.ArgumentMatchers.any());
    }
}
