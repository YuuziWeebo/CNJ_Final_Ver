package com.glucoze.thesismanagement.council.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;

import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.enums.CouncilRole;
import com.glucoze.thesismanagement.council.dto.ScheduleForm;
import com.glucoze.thesismanagement.council.entity.Council;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.council.entity.CouncilMember;
import com.glucoze.thesismanagement.council.repository.CouncilRepository;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.user.entity.Lecturer;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.audit.AuditLogService;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CouncilSchedulingServiceTest {

    @Mock
    private CouncilRepository councilRepository;
    @Mock
    private CouncilMemberRepository memberRepository;
    @Mock
    private DefenseScheduleRepository defenseScheduleRepository;
    @Mock
    private LecturerRepository lecturerRepository;
    @Mock
    private ThesisRepository thesisRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditLogService auditLogService;

    private CouncilSchedulingService service;
    private Council council;
    private Thesis thesis;
    private List<CouncilMember> members;

    @BeforeEach
    void setUp() {
        service = new CouncilSchedulingService(councilRepository, memberRepository, defenseScheduleRepository,
                lecturerRepository, thesisRepository, registrationRepository, notificationService, auditLogService);
        council = mock(Council.class);
        thesis = mock(Thesis.class);
        when(council.getId()).thenReturn(2L);
        when(thesis.getId()).thenReturn(1L);
        when(councilRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(council));
        members = List.of(
                new CouncilMember(council, lecturer(11L, "Chair", "GV01"), CouncilRole.CHAIR),
                new CouncilMember(council, lecturer(12L, "Secretary", "GV02"), CouncilRole.SECRETARY),
                new CouncilMember(council, lecturer(13L, "Member", "GV03"), CouncilRole.MEMBER));
        when(thesisRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(thesis));
        when(councilRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(council));
        when(registrationRepository.findFirstByThesisIdAndStatus(1L, RegistrationStatus.APPROVED))
                .thenReturn(Optional.of(mock(com.glucoze.thesismanagement.thesis.entity.Registration.class)));
        when(memberRepository.findByCouncilId(2L)).thenReturn(members);
        when(lecturerRepository.findAllByIdForUpdate(any())).thenReturn(
                members.stream().map(CouncilMember::getLecturer).toList());
        when(defenseScheduleRepository.findCouncilConflicts(any(), any(), any())).thenReturn(List.of());
        when(defenseScheduleRepository.findRoomConflicts(any(), any(), any())).thenReturn(List.of());
        when(defenseScheduleRepository.findLecturerConflicts(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void rejectsCouncilConflictWithBuffer() {
        when(defenseScheduleRepository.findCouncilConflicts(any(), any(), any()))
                .thenReturn(List.of(existingSchedule()));

        assertThatThrownBy(() -> service.createSchedule(form()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Council");
        verify(defenseScheduleRepository, never()).saveAndFlush(any(DefenseSchedule.class));
    }

    @Test
    void rejectsRoomConflictWithBuffer() {
        when(defenseScheduleRepository.findRoomConflicts(any(), any(), any()))
                .thenReturn(List.of(existingSchedule()));

        assertThatThrownBy(() -> service.createSchedule(form()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Room");
        verify(defenseScheduleRepository, never()).saveAndFlush(any(DefenseSchedule.class));
    }

    @Test
    void rejectsLecturerConflictAcrossCouncils() {
        when(defenseScheduleRepository.findLecturerConflicts(any(), any(), any()))
                .thenReturn(List.of(existingSchedule()));

        assertThatThrownBy(() -> service.createSchedule(form()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lecturer");
        verify(defenseScheduleRepository, never()).saveAndFlush(any(DefenseSchedule.class));
    }

    @Test
    void rejectsScheduleForUnapprovedThesis() {
        when(registrationRepository.findFirstByThesisIdAndStatus(1L, RegistrationStatus.APPROVED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSchedule(form()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approved thesis");
    }

    @Test
    void rejectsSchedulingThesisThatAlreadyHasASession() {
        when(defenseScheduleRepository.existsByThesisId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.createSchedule(form()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has a defense schedule");
        verify(defenseScheduleRepository, never()).saveAndFlush(any(DefenseSchedule.class));
    }

    @Test
    void protectsRequiredChairpersonFromRemoval() {
        when(councilRepository.findById(2L)).thenReturn(Optional.of(council));
        CouncilMember chair = mock(CouncilMember.class);
        when(chair.getId()).thenReturn(21L);
        when(chair.getRole()).thenReturn(CouncilRole.CHAIR);
        CouncilMember secretary = mock(CouncilMember.class);
        when(secretary.getRole()).thenReturn(CouncilRole.SECRETARY);
        CouncilMember member = mock(CouncilMember.class);
        when(member.getRole()).thenReturn(CouncilRole.MEMBER);
        CouncilMember extra = mock(CouncilMember.class);
        when(extra.getRole()).thenReturn(CouncilRole.MEMBER);
        when(memberRepository.findByCouncilId(2L)).thenReturn(List.of(chair, secretary, member, extra));

        assertThatThrownBy(() -> service.deleteMember(2L, 21L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chairperson and Secretary");
        verify(memberRepository, never()).delete(chair);
    }

    @Test
    void expandsRepositoryCandidateWindowExactlyOnce() {
        service.createSchedule(form());

        ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(defenseScheduleRepository).findCouncilConflicts(eq(2L), start.capture(), end.capture());
        org.assertj.core.api.Assertions.assertThat(start.getValue())
                .isEqualTo(LocalDateTime.of(2026, 8, 20, 10, 0));
        org.assertj.core.api.Assertions.assertThat(end.getValue())
                .isEqualTo(LocalDateTime.of(2026, 8, 20, 11, 30));
    }

    @Test
    void locksThesisCouncilAndMembersBeforeCheckingConflicts() {
        service.createSchedule(form());

        var ordered = inOrder(thesisRepository, councilRepository, lecturerRepository, defenseScheduleRepository);
        ordered.verify(councilRepository).findFirstByOrderByIdAsc();
        ordered.verify(thesisRepository).findByIdForUpdate(1L);
        ordered.verify(councilRepository).findByIdForUpdate(2L);
        ordered.verify(lecturerRepository).findAllByIdForUpdate(List.of(11L, 12L, 13L));
        ordered.verify(defenseScheduleRepository).findCouncilConflicts(eq(2L), any(), any());
        verify(defenseScheduleRepository).saveAndFlush(any(DefenseSchedule.class));
    }

    @Test
    void councilListUsesOneGroupedMemberCountQueryForMultipleCouncils() {
        Council first = council(1L, "Council A");
        Council second = council(2L, "Council B");
        Council third = council(3L, "Council C");
        CouncilMemberRepository.CouncilMemberCount firstCount = memberCount(1L, 3L);
        CouncilMemberRepository.CouncilMemberCount secondCount = memberCount(2L, 4L);
        when(councilRepository.findAll()).thenReturn(List.of(first, second, third));
        when(memberRepository.countMembersByCouncilIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(firstCount, secondCount));

        var views = service.listCouncils();

        assertThat(views).extracting(view -> view.name())
                .containsExactly("Council A", "Council B", "Council C");
        assertThat(views).extracting(view -> view.memberCount())
                .containsExactly(3L, 4L, 0L);
        verify(councilRepository).findAll();
        verify(memberRepository).countMembersByCouncilIds(List.of(1L, 2L, 3L));
    }

    private Council council(Long id, String name) {
        Council value = mock(Council.class);
        when(value.getId()).thenReturn(id);
        when(value.getName()).thenReturn(name);
        when(value.getAcademicYear()).thenReturn("2026");
        when(value.getSemester()).thenReturn("1");
        return value;
    }

    private CouncilMemberRepository.CouncilMemberCount memberCount(Long councilId, long count) {
        CouncilMemberRepository.CouncilMemberCount value =
                mock(CouncilMemberRepository.CouncilMemberCount.class);
        when(value.getCouncilId()).thenReturn(councilId);
        when(value.getMemberCount()).thenReturn(count);
        return value;
    }

    private ScheduleForm form() {
        ScheduleForm form = new ScheduleForm();
        form.setThesisId(1L);
        form.setCouncilId(2L);
        form.setRoom("A101");
        form.setStartTime(LocalDateTime.of(2026, 8, 20, 10, 15));
        form.setEndTime(LocalDateTime.of(2026, 8, 20, 11, 15));
        return form;
    }

    private DefenseSchedule existingSchedule() {
        return new DefenseSchedule(null, null, "A101",
                LocalDateTime.of(2026, 8, 20, 9, 0),
                LocalDateTime.of(2026, 8, 20, 10, 0));
    }

        private Lecturer lecturer(Long id, String name, String code) {
                Lecturer lecturer = mock(Lecturer.class);
                when(lecturer.getId()).thenReturn(id);
                when(lecturer.getFullName()).thenReturn(name);
                when(lecturer.getLecturerCode()).thenReturn(code);
                return lecturer;
        }
}
