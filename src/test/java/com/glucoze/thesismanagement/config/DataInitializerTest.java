package com.glucoze.thesismanagement.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.council.repository.CouncilRepository;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.grading.repository.GradeRepository;
import com.glucoze.thesismanagement.grading.service.GradingCalculator;
import com.glucoze.thesismanagement.grading.service.GradingService;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ReportRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Test
    void isOnlyActiveForDevelopmentProfile() {
        assertThat(DataInitializer.class.getAnnotation(Profile.class).value()).containsExactly("dev");
    }

    @Test
    void isCreatedOnlyWhenDevelopmentProfileIsActive() {
        new ApplicationContextRunner()
                .withUserConfiguration(NonDevInitializerConfiguration.class)
                .run(context -> assertThat(context.containsBean("dataInitializer")).isFalse());
    }

    @Configuration(proxyBeanMethods = false)
    @Import(DataInitializer.class)
    static class NonDevInitializerConfiguration {
    }

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private LecturerRepository lecturerRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock private ThesisRepository thesisRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private ReportRepository reportRepository;
    @Mock private CouncilRepository councilRepository;
    @Mock private CouncilMemberRepository memberRepository;
    @Mock private DefenseScheduleRepository scheduleRepository;
    @Mock private GradeRepository gradeRepository;
    @Mock private GradingCalculator gradingCalculator;
    @Mock private GradingService gradingService;

    @Test
    void encodesConfiguredDemoPasswordWhenInitializingDevelopmentAccounts() {
        Map<String, com.glucoze.thesismanagement.user.entity.UserAccount> accounts = new HashMap<>();
        when(userAccountRepository.findByUsername(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(accounts.get(invocation.getArgument(0))));
        when(userAccountRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            var account = (com.glucoze.thesismanagement.user.entity.UserAccount) invocation.getArgument(0);
            accounts.put(account.getUsername(), account);
            return account;
        });
        when(adminRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lecturerRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(thesisRepository.count()).thenReturn(1L);
        when(passwordEncoder.encode(DataInitializer.ADMIN_PASSWORD)).thenReturn("hashed-admin");
        when(passwordEncoder.encode(DataInitializer.LECTURER_PASSWORD)).thenReturn("hashed-lecturer");
        when(passwordEncoder.encode(DataInitializer.STUDENT_PASSWORD)).thenReturn("hashed-student");

        DataInitializer initializer = new DataInitializer(
                userAccountRepository,
                adminRepository,
                lecturerRepository,
                studentRepository,
                thesisRepository,
                registrationRepository,
                reportRepository,
                councilRepository,
                memberRepository,
                scheduleRepository,
                gradeRepository,
                passwordEncoder,
                gradingCalculator,
                gradingService);

        initializer.initializeDemoDataset();

        verify(userAccountRepository).save(argThat(account ->
                account.getUsername().equals("admin")
                        && account.getRole() == Role.ADMIN
                        && account.getPasswordHash().equals("hashed-admin")));
        verify(userAccountRepository).save(argThat(account ->
                account.getUsername().equals("lecturer01")
                        && account.getRole() == Role.LECTURER
                        && account.getPasswordHash().equals("hashed-lecturer")));
        verify(userAccountRepository).save(argThat(account ->
                account.getUsername().equals("student01")
                        && account.getRole() == Role.STUDENT
                        && account.getPasswordHash().equals("hashed-student")));
    }
}
