package com.glucoze.thesismanagement.grading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.enums.ScoreSource;
import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.common.enums.CouncilRole;
import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.council.entity.Council;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.council.entity.CouncilMember;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.grading.config.GradingProperties;
import com.glucoze.thesismanagement.grading.dto.ScoreForm;
import com.glucoze.thesismanagement.grading.entity.Grade;
import com.glucoze.thesismanagement.grading.entity.Result;
import com.glucoze.thesismanagement.grading.repository.GradeRepository;
import com.glucoze.thesismanagement.grading.repository.ResultRepository;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.audit.AuditLogService;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GradingServiceTest {

    @Mock private DefenseScheduleRepository scheduleRepository;
    @Mock private GradeRepository scoreRepository;
    @Mock private ResultRepository resultRepository;
    @Mock private CouncilMemberRepository memberRepository;
    @Mock private LecturerRepository lecturerRepository;
    @Mock private RegistrationRepository registrationRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;

    private GradingService service;
    private DefenseSchedule schedule;
    private Lecturer supervisor;

    @BeforeEach
    void setUp() {
        service = new GradingService(scheduleRepository, scoreRepository, resultRepository, memberRepository,
                lecturerRepository, new GradingCalculator(new GradingProperties()), registrationRepository,
                notificationService, auditLogService);
        schedule = mock(DefenseSchedule.class);
        Thesis thesis = mock(Thesis.class);
        Council council = mock(Council.class);
        supervisor = lecturer(10L, "supervisor01");
        when(schedule.getId()).thenReturn(1L);
        when(schedule.getThesis()).thenReturn(thesis);
        when(schedule.getCouncil()).thenReturn(council);
        when(thesis.getSupervisor()).thenReturn(supervisor);
        when(council.getId()).thenReturn(20L);
        when(scheduleRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(schedule));
        when(resultRepository.findByDefenseScheduleId(1L)).thenReturn(Optional.empty());
    }

    @Test
    void councilMemberCanOnlyEnterOwnScore() {
        List<CouncilMember> members = completeCouncil();
        Lecturer chair = members.get(0).getLecturer();
        when(memberRepository.findByCouncilId(20L)).thenReturn(members);
        when(memberRepository.existsByCouncilIdAndLecturerId(20L, 30L)).thenReturn(true);
        when(lecturerRepository.findById(30L)).thenReturn(Optional.of(chair));

        service.enterCouncilScore("chair", 1L, score("8.0"));

        ArgumentCaptor<Grade> saved = ArgumentCaptor.forClass(Grade.class);
        verify(scoreRepository).save(saved.capture());
        assertThat(saved.getValue().getLecturer().getId()).isEqualTo(30L);
    }

    @Test
    void unrelatedLecturerCannotEnterCouncilScore() {
        List<CouncilMember> members = completeCouncil();
        when(memberRepository.findByCouncilId(20L)).thenReturn(members);

        assertThatThrownBy(() -> service.enterCouncilScore("outsider", 1L, score("8.0")))
                .isInstanceOf(DomainRuleViolationException.class);
        verify(scoreRepository, never()).save(any(Grade.class));
    }

    @Test
    void zeroOfThreeCouncilScoresCannotPublish() {
        assertIncompleteScoresRejected(List.of());
    }

    @Test
    void oneOfThreeCouncilScoresCannotPublish() {
        assertIncompleteScoresRejected(List.of(councilScore(30L, "8.0")));
    }

    @Test
    void twoOfThreeCouncilScoresCannotPublish() {
        assertIncompleteScoresRejected(List.of(councilScore(30L, "8.0"), councilScore(31L, "9.0")));
    }

    @Test
    void chairCanPublishWithScoresFromExactlyAllThreeMembers() {
        preparePublish("chair", CouncilRole.CHAIR, validCouncilScores());
        com.glucoze.thesismanagement.thesis.entity.Registration registration =
                mock(com.glucoze.thesismanagement.thesis.entity.Registration.class);
        when(registrationRepository.findFirstByThesisIdAndStatus(
                schedule.getThesis().getId(), com.glucoze.thesismanagement.common.enums.RegistrationStatus.APPROVED))
                .thenReturn(Optional.of(registration));

        service.publishResult("chair", 1L);

        verify(resultRepository).save(any(Result.class));
        verify(notificationService).resultPublished(registration);
        verify(auditLogService).append(com.glucoze.thesismanagement.audit.AuditAction.RESULT_PUBLISH,
                com.glucoze.thesismanagement.audit.AuditEntityType.RESULT, null,
                "Đã công bố kết quả cho lịch bảo vệ #1");
    }

    @Test
    void secretaryCanPublishWithScoresFromExactlyAllThreeMembers() {
        preparePublish("secretary", CouncilRole.SECRETARY, validCouncilScores());

        service.publishResult("secretary", 1L);

        verify(resultRepository).save(any(Result.class));
    }

    @Test
    void threeScoresIncludingOutsiderCannotPublish() {
        preparePublish("chair", CouncilRole.CHAIR,
                List.of(councilScore(30L, "8.0"), councilScore(31L, "9.0"), councilScore(99L, "7.0")));

        assertThatThrownBy(() -> service.publishResult("chair", 1L))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("đúng 3 thành viên");
        verify(resultRepository, never()).save(any(Result.class));
    }

    @Test
    void duplicateCouncilScorerCannotSatisfyCompleteness() {
        preparePublish("chair", CouncilRole.CHAIR,
                List.of(councilScore(30L, "8.0"), councilScore(30L, "9.0"), councilScore(31L, "7.0")));

        assertThatThrownBy(() -> service.publishResult("chair", 1L))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("đúng 3 thành viên");
        verify(resultRepository, never()).save(any(Result.class));
    }

    @Test
    void abnormalCouncilMembershipCannotPublish() {
        List<CouncilMember> twoMembers = List.of(
                member(30L, "chair", CouncilRole.CHAIR), member(31L, "secretary", CouncilRole.SECRETARY));
        when(memberRepository.findByCouncilId(20L)).thenReturn(twoMembers);

        assertThatThrownBy(() -> service.publishResult("chair", 1L))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("đúng Chủ tịch, Thư ký và Ủy viên");
        verify(resultRepository, never()).save(any(Result.class));
    }

    @Test
    void ordinaryMemberCannotPublish() {
        preparePublish("member", CouncilRole.MEMBER, validCouncilScores());

        assertThatThrownBy(() -> service.publishResult("member", 1L))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("Ủy viên");
        verify(resultRepository, never()).save(any(Result.class));
    }

    @Test
    void supervisorOutsideCouncilCannotPublish() {
        preparePublish("chair", CouncilRole.CHAIR, validCouncilScores());

        assertThatThrownBy(() -> service.publishResult("supervisor01", 1L))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("Chủ tịch hoặc Thư ký");
        verify(resultRepository, never()).save(any(Result.class));
    }

    @Test
    void lecturerFromAnotherCouncilCannotPublish() {
        preparePublish("chair", CouncilRole.CHAIR, validCouncilScores());

        assertThatThrownBy(() -> service.publishResult("other-council", 1L))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("hội đồng này");
        verify(resultRepository, never()).save(any(Result.class));
    }

    @Test
    void publishedResultPreventsFurtherScoreChanges() {
        Result published = mock(Result.class);
        when(published.isPublished()).thenReturn(true);
        when(resultRepository.findByDefenseScheduleId(1L)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> service.enterCouncilScore("chair", 1L, score("9.0")))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("không thể thêm hoặc sửa điểm");
        verify(scoreRepository, never()).save(any(Grade.class));
    }

    @Test
    void publishedResultCannotBePublishedAgain() {
        Result published = mock(Result.class);
        when(published.isPublished()).thenReturn(true);
        when(resultRepository.findByDefenseScheduleId(1L)).thenReturn(Optional.of(published));

        assertThatThrownBy(() -> service.publishResult("chair", 1L))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("không thể thay đổi");
        verify(resultRepository, never()).save(any(Result.class));
    }

    private void assertIncompleteScoresRejected(List<Grade> scores) {
        preparePublish("chair", CouncilRole.CHAIR, scores);
        assertThatThrownBy(() -> service.publishResult("chair", 1L))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("đủ điểm");
        verify(resultRepository, never()).save(any(Result.class));
    }

    private void preparePublish(String actor, CouncilRole actorRole, List<Grade> councilScores) {
        List<CouncilMember> members = completeCouncil();
        Grade supervisorScore = supervisorScore("8.0");
        when(memberRepository.findByCouncilId(20L)).thenReturn(members);
        when(scoreRepository.findAllByDefenseScheduleIdAndScoreSource(1L, ScoreSource.SUPERVISOR))
                .thenReturn(List.of(supervisorScore));
        when(scoreRepository.findAllByDefenseScheduleIdAndScoreSource(1L, ScoreSource.COUNCIL))
                .thenReturn(councilScores);
        assertThat(members.stream().anyMatch(member -> member.getRole() == actorRole
                && member.getLecturer().getUserAccount().getUsername().equals(actor))).isTrue();
    }

    private List<CouncilMember> completeCouncil() {
        return List.of(
                member(30L, "chair", CouncilRole.CHAIR),
                member(31L, "secretary", CouncilRole.SECRETARY),
                member(32L, "member", CouncilRole.MEMBER));
    }

    private List<Grade> validCouncilScores() {
        return List.of(councilScore(30L, "8.0"), councilScore(31L, "9.0"), councilScore(32L, "7.0"));
    }

    private CouncilMember member(Long id, String username, CouncilRole role) {
        CouncilMember member = mock(CouncilMember.class);
        Lecturer lecturer = lecturer(id, username);
        when(member.getLecturer()).thenReturn(lecturer);
        when(member.getRole()).thenReturn(role);
        return member;
    }

    private Lecturer lecturer(Long id, String username) {
        Lecturer lecturer = mock(Lecturer.class);
        when(lecturer.getId()).thenReturn(id);
        when(lecturer.getUserAccount()).thenReturn(new UserAccount(username, "hash", Role.LECTURER));
        return lecturer;
    }

    private Grade councilScore(Long lecturerId, String value) {
        Grade score = mock(Grade.class);
        Lecturer lecturer = lecturer(lecturerId, "score-" + lecturerId);
        when(score.getLecturer()).thenReturn(lecturer);
        when(score.getScore()).thenReturn(new BigDecimal(value));
        return score;
    }

    private Grade supervisorScore(String value) {
        Grade score = mock(Grade.class);
        when(score.getScore()).thenReturn(new BigDecimal(value));
        return score;
    }

    private ScoreForm score(String value) {
        ScoreForm form = new ScoreForm();
        form.setScore(new BigDecimal(value));
        return form;
    }

}
