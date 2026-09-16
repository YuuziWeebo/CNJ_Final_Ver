package com.glucoze.thesismanagement.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.user.dto.AdminUserEditForm;
import com.glucoze.thesismanagement.user.dto.AdminUserForm;
import com.glucoze.thesismanagement.user.entity.Admin;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.Student;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.grading.repository.GradeRepository;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import com.glucoze.thesismanagement.audit.AuditLogService;
import com.glucoze.thesismanagement.audit.AuditAction;
import com.glucoze.thesismanagement.audit.AuditEntityType;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private LecturerRepository lecturerRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private ThesisRepository thesisRepository;

    @Mock
    private CouncilMemberRepository councilMemberRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock private AuditLogService auditLogService;


    private UserManagementService service;

    @BeforeEach
    void setUp() {
        service = new UserManagementService(userAccountRepository, studentRepository, lecturerRepository,
                adminRepository, registrationRepository, thesisRepository, councilMemberRepository,
                gradeRepository, passwordEncoder, auditLogService);
    }

    @Test
    void adminCannotDisableOwnAccount() {
        UserAccount account = new UserAccount("admin01", "hash", Role.ADMIN);
        AdminUserEditForm form = new AdminUserEditForm();
        form.setId(1L);
        form.setEnabled(false);

        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.updateAccount(form, "admin01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khóa tài khoản của chính mình");
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    void adminCannotDeleteOwnAccount() {
        UserAccount account = new UserAccount("admin01", "hash", Role.ADMIN);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.deleteAccount(1L, "admin01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("xóa tài khoản của chính mình");
        verify(userAccountRepository, never()).delete(any(UserAccount.class));
    }

    @Test
    void refusesStudentDeletionWhenRegistrationExists() {
        UserAccount account = new UserAccount("student01", "hash", Role.STUDENT);
        Student student = new Student(account, "Student", "SV01", "C1");
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(studentRepository.findByUserAccountUsername("student01")).thenReturn(Optional.of(student));
        when(registrationRepository.existsByStudentId(null)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteAccount(1L, "admin01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đăng ký đề tài");
        verify(userAccountRepository, never()).delete(any(UserAccount.class));
    }

    @Test
    void refusesLecturerDeletionWhenSupervisedThesisExists() {
        UserAccount account = new UserAccount("lecturer01", "hash", Role.LECTURER);
        Lecturer lecturer = new Lecturer(account, "Lecturer", "GV01", "Department");
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(lecturerRepository.findByUserAccountUsername("lecturer01")).thenReturn(Optional.of(lecturer));
        when(thesisRepository.existsBySupervisorId(null)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteAccount(1L, "admin01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đề tài, hội đồng hoặc điểm chấm");
        verify(userAccountRepository, never()).delete(any(UserAccount.class));
    }

    @Test
    void refusesLecturerDeletionWhenCouncilMembershipExists() {
        UserAccount account = new UserAccount("lecturer01", "hash", Role.LECTURER);
        Lecturer lecturer = new Lecturer(account, "Lecturer", "GV01", "Department");
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(lecturerRepository.findByUserAccountUsername("lecturer01")).thenReturn(Optional.of(lecturer));
        when(councilMemberRepository.existsByLecturerId(null)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteAccount(1L, "admin01"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userAccountRepository, never()).delete(any(UserAccount.class));
    }

    @Test
    void refusesLecturerDeletionWhenGradingRecordExists() {
        UserAccount account = new UserAccount("lecturer01", "hash", Role.LECTURER);
        Lecturer lecturer = new Lecturer(account, "Lecturer", "GV01", "Department");
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(lecturerRepository.findByUserAccountUsername("lecturer01")).thenReturn(Optional.of(lecturer));
        when(gradeRepository.existsByLecturerId(null)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteAccount(1L, "admin01"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(userAccountRepository, never()).delete(any(UserAccount.class));
    }

    @Test
    void allowsUnreferencedAdminDeletion() {
        UserAccount account = new UserAccount("admin02", "hash", Role.ADMIN);
        Admin admin = new Admin(account, "Admin");
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(adminRepository.findByUserAccountUsername("admin02")).thenReturn(Optional.of(admin));

        service.deleteAccount(1L, "admin01");

        verify(adminRepository).delete(admin);
        verify(userAccountRepository).delete(account);
    }

    @Test
    void accountSummariesUseConstantBulkProfileQueriesAndPreserveOutput() {
        UserAccount studentAccount = account(1L, "student", Role.STUDENT);
        UserAccount lecturerAccount = account(2L, "lecturer", Role.LECTURER);
        UserAccount adminAccount = account(3L, "admin", Role.ADMIN);
        Student student = mock(Student.class);
        Lecturer lecturer = mock(Lecturer.class);
        Admin admin = mock(Admin.class);
        when(student.getUserAccount()).thenReturn(studentAccount);
        when(student.getFullName()).thenReturn("Student Name");
        when(lecturer.getUserAccount()).thenReturn(lecturerAccount);
        when(lecturer.getFullName()).thenReturn("Lecturer Name");
        when(admin.getUserAccount()).thenReturn(adminAccount);
        when(admin.getFullName()).thenReturn("Admin Name");
        List<Long> accountIds = List.of(1L, 2L, 3L);
        when(userAccountRepository.findAll()).thenReturn(List.of(studentAccount, lecturerAccount, adminAccount));
        when(studentRepository.findByUserAccountIdIn(accountIds)).thenReturn(List.of(student));
        when(lecturerRepository.findByUserAccountIdIn(accountIds)).thenReturn(List.of(lecturer));
        when(adminRepository.findByUserAccountIdIn(accountIds)).thenReturn(List.of(admin));

        var summaries = service.listAccounts();

        assertThat(summaries).extracting(summary -> summary.fullName())
                .containsExactly("Student Name", "Lecturer Name", "Admin Name");
        verify(studentRepository).findByUserAccountIdIn(accountIds);
        verify(lecturerRepository).findByUserAccountIdIn(accountIds);
        verify(adminRepository).findByUserAccountIdIn(accountIds);
        verify(studentRepository, never()).findByUserAccountUsername(any());
        verify(lecturerRepository, never()).findByUserAccountUsername(any());
        verify(adminRepository, never()).findByUserAccountUsername(any());
    }

    @Test
    void createsStudentWithTheValidatedNormalizedProfileCode() {
        AdminUserForm form = new AdminUserForm();
        form.setUsername("student02");
        form.setPassword("secret123");
        form.setFullName("Student Two");
        form.setRole(Role.STUDENT);
        form.setStudentCode("  SV02  ");
        form.setClassName(" C2 ");
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userAccountRepository.save(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createAccount(form);

        ArgumentCaptor<Student> profile = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).existsByStudentCode("SV02");
        verify(studentRepository).save(profile.capture());
        assertThat(profile.getValue().getStudentCode()).isEqualTo("SV02");
        assertThat(profile.getValue().getClassName()).isEqualTo("C2");
        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).append(eq(AuditAction.USER_CREATE), eq(AuditEntityType.USER_ACCOUNT),
                any(), description.capture());
        assertThat(description.getValue()).doesNotContain("secret123", "encoded", "hash", "token");
    }

    @Test
    void createsLecturerWithTheValidatedNormalizedProfileCode() {
        AdminUserForm form = new AdminUserForm();
        form.setUsername("lecturer02");
        form.setPassword("secret123");
        form.setFullName("Lecturer Two");
        form.setRole(Role.LECTURER);
        form.setLecturerCode("  GV02  ");
        form.setDepartment(" IT ");
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userAccountRepository.save(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createAccount(form);

        ArgumentCaptor<Lecturer> profile = ArgumentCaptor.forClass(Lecturer.class);
        verify(lecturerRepository).existsByLecturerCode("GV02");
        verify(lecturerRepository).save(profile.capture());
        assertThat(profile.getValue().getLecturerCode()).isEqualTo("GV02");
        assertThat(profile.getValue().getDepartment()).isEqualTo("IT");
    }

    @Test
    void passwordResetAuditNeverContainsRawOrEncodedPassword() {
        UserAccount account = mock(UserAccount.class);
        Admin admin = mock(Admin.class);
        when(account.getId()).thenReturn(9L);
        when(account.getUsername()).thenReturn("target-admin");
        when(account.getRole()).thenReturn(Role.ADMIN);
        when(account.isEnabled()).thenReturn(true);
        when(userAccountRepository.findById(9L)).thenReturn(Optional.of(account));
        when(adminRepository.findByUserAccountUsername("target-admin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.encode("RawSecret!123")).thenReturn("$2a$encoded-hash");
        AdminUserEditForm form = new AdminUserEditForm();
        form.setId(9L);
        form.setEnabled(true);
        form.setFullName("Target Admin");
        form.setNewPassword("RawSecret!123");

        service.updateAccount(form, "admin01");

        ArgumentCaptor<String> description = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).append(eq(AuditAction.USER_PASSWORD_RESET), eq(AuditEntityType.USER_ACCOUNT),
                eq(9L), description.capture());
        assertThat(description.getValue()).doesNotContain("RawSecret!123", "$2a$encoded-hash");
    }

    private UserAccount account(Long id, String username, Role role) {
        UserAccount account = mock(UserAccount.class);
        when(account.getId()).thenReturn(id);
        when(account.getUsername()).thenReturn(username);
        when(account.getRole()).thenReturn(role);
        when(account.isEnabled()).thenReturn(true);
        return account;
    }
}
