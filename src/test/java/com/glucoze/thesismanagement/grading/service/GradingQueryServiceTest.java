package com.glucoze.thesismanagement.grading.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import com.glucoze.thesismanagement.council.entity.Council;
import com.glucoze.thesismanagement.council.entity.DefenseSchedule;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.council.repository.CouncilMemberRepository;
import com.glucoze.thesismanagement.grading.dto.GradeView;
import com.glucoze.thesismanagement.grading.repository.GradeRepository;
import com.glucoze.thesismanagement.grading.repository.ResultRepository;
import com.glucoze.thesismanagement.thesis.entity.Thesis;
import com.glucoze.thesismanagement.thesis.repository.RegistrationRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GradingQueryServiceTest {

    @Mock private DefenseScheduleRepository scheduleRepository;
    @Mock private GradeRepository scoreRepository;
    @Mock private ResultRepository resultRepository;
    @Mock private CouncilMemberRepository memberRepository;
    @Mock private RegistrationRepository registrationRepository;

    private GradingQueryService service;

    @BeforeEach
    void setUp() {
        service = new GradingQueryService(
                scheduleRepository, scoreRepository, resultRepository, memberRepository, registrationRepository);
    }

    @Test
    void gradingListBulkLoadsDetailsOnceForMultipleRowsAndPreservesViews() {
        DefenseSchedule first = scheduleRow(1L, 20L, "Topic A");
        DefenseSchedule second = scheduleRow(2L, 21L, "Topic B");
        when(scheduleRepository.findByThesisSupervisorUserAccountUsername("lecturer"))
                .thenReturn(List.of(first, second));
        when(scheduleRepository.findByCouncilMemberUsername("lecturer")).thenReturn(List.of());
        when(scoreRepository.findByDefenseScheduleIdIn(List.of(1L, 2L))).thenReturn(List.of());
        when(memberRepository.findByCouncilIdIn(List.of(20L, 21L))).thenReturn(List.of());
        when(resultRepository.findByDefenseScheduleIdIn(List.of(1L, 2L))).thenReturn(List.of());

        List<GradeView> views = service.listLecturerSchedules("lecturer");

        assertThat(views).extracting(GradeView::thesisTitle).containsExactly("Topic A", "Topic B");
        verify(scoreRepository).findByDefenseScheduleIdIn(List.of(1L, 2L));
        verify(memberRepository).findByCouncilIdIn(List.of(20L, 21L));
        verify(resultRepository).findByDefenseScheduleIdIn(List.of(1L, 2L));
        verify(scoreRepository, never()).findByDefenseScheduleId(any());
        verify(memberRepository, never()).findByCouncilId(any());
        verify(resultRepository, never()).findByDefenseScheduleId(any());
    }

    @Test
    void publishedGradeDashboardCountUsesAggregateQuery() {
        when(resultRepository.countPublishedByStudent("student", RegistrationStatus.APPROVED)).thenReturn(2L);

        assertThat(service.countStudentPublishedGrades("student")).isEqualTo(2L);

        verify(resultRepository).countPublishedByStudent("student", RegistrationStatus.APPROVED);
        verify(registrationRepository, never()).findByStudentUserAccountUsernameAndStatus(any(), any());
    }

    @Test
    void lecturerCanReadOnlySchedulesTheySuperviseOrServeOn() {
        DefenseSchedule schedule = scheduleRow(1L, 20L, "Topic A");
        when(scheduleRepository.findByIdAndLecturerAccess(1L, "lecturer"))
                .thenReturn(Optional.of(schedule));
        when(scoreRepository.findByDefenseScheduleId(1L)).thenReturn(List.of());
        when(memberRepository.findByCouncilId(20L)).thenReturn(List.of());
        when(resultRepository.findByDefenseScheduleId(1L)).thenReturn(Optional.empty());

        GradeView view = service.getLecturerGrade("lecturer", 1L);

        assertThat(view.thesisTitle()).isEqualTo("Topic A");
        verify(scheduleRepository).findByIdAndLecturerAccess(1L, "lecturer");
    }

    @Test
    void unrelatedLecturerCannotReadGradingDetailByEnumeratingScheduleId() {
        when(scheduleRepository.findByIdAndLecturerAccess(99L, "unrelated"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLecturerGrade("unrelated", 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Không tìm thấy lịch chấm điểm");

        verify(scoreRepository, never()).findByDefenseScheduleId(any());
        verify(memberRepository, never()).findByCouncilId(any());
        verify(resultRepository, never()).findByDefenseScheduleId(any());
    }

    private DefenseSchedule scheduleRow(Long id, Long councilId, String title) {
        DefenseSchedule row = mock(DefenseSchedule.class);
        Council council = mock(Council.class);
        Thesis thesis = mock(Thesis.class);
        when(row.getId()).thenReturn(id);
        when(row.getCouncil()).thenReturn(council);
        when(row.getThesis()).thenReturn(thesis);
        when(council.getId()).thenReturn(councilId);
        when(thesis.getTitle()).thenReturn(title);
        return row;
    }
}
