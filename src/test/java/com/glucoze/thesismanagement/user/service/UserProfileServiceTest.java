package com.glucoze.thesismanagement.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.user.dto.StudentProfileForm;
import com.glucoze.thesismanagement.user.entity.Student;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserProfileServiceTest {

    @Test
    void studentProfileReadAndUpdateKeepExistingContract() {
        StudentRepository students = mock(StudentRepository.class);
        LecturerRepository lecturers = mock(LecturerRepository.class);
        AdminRepository admins = mock(AdminRepository.class);
        UserProfileService service = new UserProfileService(
                students, lecturers, admins, mock(UserAvatarStorage.class));
        Student student = new Student(new UserAccount("student", "hash", Role.STUDENT),
                "Tên cũ", "SV01", "KTPM");
        when(students.findByUserAccountUsername("student")).thenReturn(Optional.of(student));

        StudentProfileForm before = service.getStudentProfile("student");
        service.updateStudentProfile("student", new StudentProfileForm(" Tên mới ", "SV01", "  AI  ", null));

        assertThat(before.getFullName()).isEqualTo("Tên cũ");
        assertThat(student.getFullName()).isEqualTo("Tên mới");
        assertThat(student.getClassName()).isEqualTo("AI");
        verify(students, times(2)).findByUserAccountUsername("student");
    }
}
