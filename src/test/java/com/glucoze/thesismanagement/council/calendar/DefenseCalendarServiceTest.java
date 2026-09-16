package com.glucoze.thesismanagement.council.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.council.dto.CalendarScheduleRow;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.council.service.DefenseCalendarService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefenseCalendarServiceTest {
    @Mock DefenseScheduleRepository repository;
    private static final LocalDateTime START = LocalDateTime.of(2026, 9, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 10, 1, 0, 0);

    @Test void adminMapsOnlyProjectionFieldsAndKeepsLocalTime() {
        when(repository.findCalendarRows(START, END)).thenReturn(List.of(row()));
        var event = new DefenseCalendarService(repository).adminEvents(START, END).getFirst();
        assertThat(event.start()).isEqualTo(LocalDateTime.of(2026, 9, 11, 8, 0));
        assertThat(event.title()).isEqualTo("Đồ án quản lý khóa luận");
        assertThat(event.room()).isEqualTo("P101");
        assertThat(event.detailUrl()).isEqualTo("/admin/schedules");
    }

    @Test void lecturerAndStudentQueriesAlwaysUseAuthenticatedUsername() {
        when(repository.findLecturerCalendarRows("gv01", START, END)).thenReturn(List.of(row()));
        when(repository.findStudentCalendarRows("sv01", RegistrationStatus.APPROVED, START, END)).thenReturn(List.of(row()));
        DefenseCalendarService service = new DefenseCalendarService(repository);
        assertThat(service.lecturerEvents("gv01", START, END)).hasSize(1);
        assertThat(service.studentEvents("sv01", START, END)).hasSize(1);
        verify(repository).findLecturerCalendarRows("gv01", START, END);
        verify(repository).findStudentCalendarRows("sv01", RegistrationStatus.APPROVED, START, END);
    }

    @Test void invalidOrOversizedRangesAreRejectedBeforeQuery() {
        DefenseCalendarService service = new DefenseCalendarService(repository);
        assertThatThrownBy(() -> service.adminEvents(END, START)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.adminEvents(START, START.plusDays(367))).isInstanceOf(IllegalArgumentException.class);
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test void eventJsonHasNoGradeResultOrSecurityGraph() throws Exception {
        var event = new DefenseCalendarService(repository);
        when(repository.findCalendarRows(START, END)).thenReturn(List.of(row()));
        String json = new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .writeValueAsString(event.adminEvents(START, END).getFirst());
        assertThat(json).contains("\"start\":\"2026-09-11T08:00:00\"")
                .doesNotContain("grade", "result", "password", "userAccount", "feedback");
    }

    private CalendarScheduleRow row() {
        return new CalendarScheduleRow(1L, "Đồ án quản lý khóa luận", "Nguyễn Văn An", "Trần Thị Hương",
                "Hội đồng 1", "P101", LocalDateTime.of(2026, 9, 11, 8, 0), LocalDateTime.of(2026, 9, 11, 10, 30));
    }
}
