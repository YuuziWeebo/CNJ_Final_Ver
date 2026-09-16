package com.glucoze.thesismanagement.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ConcurrentModel;

class CurrentUserModelAdviceTest {

    @Test
    void resolvesNameAndAvatarFromOneAccountLookupAndOneRoleProfileLookup() {
        UserAccountRepository accounts = mock(UserAccountRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        LecturerRepository lecturers = mock(LecturerRepository.class);
        AdminRepository admins = mock(AdminRepository.class);
        UserDetails principal = mock(UserDetails.class);
        UserAccount account = mock(UserAccount.class);
        Lecturer lecturer = mock(Lecturer.class);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 9, 6, 8, 0);
        when(principal.getUsername()).thenReturn("lecturer");
        when(accounts.findByUsername("lecturer")).thenReturn(Optional.of(account));
        when(account.getUsername()).thenReturn("lecturer");
        when(account.getRole()).thenReturn(Role.LECTURER);
        when(account.getAvatarData()).thenReturn(new byte[] {1});
        when(account.getUpdatedAt()).thenReturn(updatedAt);
        when(lecturers.findByUserAccountUsername("lecturer")).thenReturn(Optional.of(lecturer));
        when(lecturer.getFullName()).thenReturn("Lecturer Name");
        ConcurrentModel model = new ConcurrentModel();

        new CurrentUserModelAdvice(accounts, admins, lecturers, students).currentUserMetadata(principal, model);

        assertThat(model.getAttribute("currentUserName")).isEqualTo("Lecturer Name");
        assertThat(model.getAttribute("currentAvatarUrl")).isEqualTo("/user/avatar?v=" + updatedAt);
        verify(accounts).findByUsername("lecturer");
        verify(lecturers).findByUserAccountUsername("lecturer");
        verify(students, never()).findByUserAccountUsername("lecturer");
        verify(admins, never()).findByUserAccountUsername("lecturer");
    }
}
