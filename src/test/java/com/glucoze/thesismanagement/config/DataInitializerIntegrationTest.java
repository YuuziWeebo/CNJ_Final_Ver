package com.glucoze.thesismanagement.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.glucoze.thesismanagement.audit.AuditLogService;
import com.glucoze.thesismanagement.common.enums.CouncilRole;
import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.enums.ScoreSource;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.council.repository.CouncilRepository;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.grading.config.GradingProperties;
import com.glucoze.thesismanagement.grading.repository.GradeRepository;
import com.glucoze.thesismanagement.grading.repository.ResultRepository;
import com.glucoze.thesismanagement.grading.service.GradingCalculator;
import com.glucoze.thesismanagement.grading.service.GradingService;
import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ReportRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("dev")
@Import({DataInitializer.class, GradingCalculator.class, GradingService.class,
        DataInitializerIntegrationTest.TestConfig.class})
class DataInitializerIntegrationTest {

    @MockBean NotificationService notificationService;
    @MockBean AuditLogService auditLogService;

    @Autowired DataInitializer initializer;
    @Autowired UserAccountRepository accountRepository;
    @Autowired AdminRepository adminRepository;
    @Autowired LecturerRepository lecturerRepository;
    @Autowired StudentRepository studentRepository;
    @Autowired ThesisRepository thesisRepository;
    @Autowired RegistrationRepository registrationRepository;
    @Autowired ReportRepository reportRepository;
    @Autowired CouncilRepository councilRepository;
    @Autowired CouncilMemberRepository memberRepository;
    @Autowired DefenseScheduleRepository scheduleRepository;
    @Autowired GradeRepository gradeRepository;
    @Autowired ResultRepository resultRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void initializesCompleteValidDatasetAndDoesNotDuplicateIt() {
        initializer.initializeDemoDataset();

        assertThat(accountRepository.count()).isEqualTo(13);
        assertThat(adminRepository.count()).isEqualTo(1);
        assertThat(lecturerRepository.count()).isEqualTo(6);
        assertThat(studentRepository.count()).isEqualTo(6);
        assertThat(thesisRepository.count()).isEqualTo(8);
        assertThat(registrationRepository.count()).isEqualTo(7);
        assertThat(reportRepository.count()).isEqualTo(4);
        assertThat(councilRepository.count()).isEqualTo(3);
        assertThat(memberRepository.count()).isEqualTo(9);
        assertThat(scheduleRepository.count()).isEqualTo(4);
        assertThat(gradeRepository.count()).isEqualTo(10);
        assertThat(resultRepository.count()).isEqualTo(1);

        councilRepository.findAll().forEach(council -> {
            var members = memberRepository.findByCouncilId(council.getId());
            assertThat(members).hasSize(3);
            assertThat(members).extracting(member -> member.getLecturer().getId()).doesNotHaveDuplicates();
            assertThat(members).extracting(member -> member.getRole())
                    .containsExactlyInAnyOrderElementsOf(Set.of(CouncilRole.CHAIR, CouncilRole.SECRETARY, CouncilRole.MEMBER));
        });

        var published = resultRepository.findAll().getFirst();
        assertThat(published.isPublished()).isTrue();
        assertThat(published.getFinalScore()).isEqualByComparingTo("8.7");
        var publishedSchedule = published.getDefenseSchedule();
        assertThat(registrationRepository.existsByThesisIdAndStatus(
                publishedSchedule.getThesis().getId(), RegistrationStatus.APPROVED)).isTrue();
        assertThat(gradeRepository.findAllByDefenseScheduleIdAndScoreSource(
                publishedSchedule.getId(), ScoreSource.SUPERVISOR)).hasSize(1);
        var councilScorerIds = gradeRepository.findAllByDefenseScheduleIdAndScoreSource(
                        publishedSchedule.getId(), ScoreSource.COUNCIL).stream()
                .map(grade -> grade.getLecturer().getId())
                .collect(Collectors.toSet());
        var memberIds = memberRepository.findByCouncilId(publishedSchedule.getCouncil().getId()).stream()
                .map(member -> member.getLecturer().getId())
                .collect(Collectors.toSet());
        assertThat(councilScorerIds).hasSize(3).isEqualTo(memberIds);

        assertThat(passwordEncoder.matches(DataInitializer.ADMIN_PASSWORD,
                accountRepository.findByUsername("admin").orElseThrow().getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(DataInitializer.LECTURER_PASSWORD,
                accountRepository.findByUsername("lecturer01").orElseThrow().getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(DataInitializer.STUDENT_PASSWORD,
                accountRepository.findByUsername("student01").orElseThrow().getPasswordHash())).isTrue();

        initializer.initializeDemoDataset();
        assertThat(accountRepository.count()).isEqualTo(13);
        assertThat(thesisRepository.count()).isEqualTo(8);
        assertThat(scheduleRepository.count()).isEqualTo(4);
        assertThat(gradeRepository.count()).isEqualTo(10);
    }

    @TestConfiguration
    @EnableConfigurationProperties(GradingProperties.class)
    static class TestConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
