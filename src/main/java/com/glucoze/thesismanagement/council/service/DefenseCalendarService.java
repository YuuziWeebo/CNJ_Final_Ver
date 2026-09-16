package com.glucoze.thesismanagement.council.service;

import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.council.dto.CalendarEventView;
import com.glucoze.thesismanagement.council.dto.CalendarScheduleRow;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DefenseCalendarService {
    static final long MAX_RANGE_DAYS = 366;
    private final DefenseScheduleRepository repository;

    public DefenseCalendarService(DefenseScheduleRepository repository) { this.repository = repository; }

    public List<CalendarEventView> adminEvents(LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);
        return map(repository.findCalendarRows(start, end), "/admin/schedules");
    }

    public List<CalendarEventView> lecturerEvents(String username, LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);
        return map(repository.findLecturerCalendarRows(username, start, end), "/lecturer/grading/");
    }

    public List<CalendarEventView> studentEvents(String username, LocalDateTime start, LocalDateTime end) {
        validateRange(start, end);
        return map(repository.findStudentCalendarRows(username, RegistrationStatus.APPROVED, start, end),
                "/student/theses");
    }

    private List<CalendarEventView> map(List<CalendarScheduleRow> rows, String detailBase) {
        return rows.stream().map(row -> new CalendarEventView(row.id(), row.thesisTitle(), row.start(), row.end(),
                row.room(), row.councilName(), row.supervisorName(), row.studentName(),
                detailBase.endsWith("/") ? detailBase + row.id() : detailBase)).toList();
    }

    private void validateRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new DomainRuleViolationException("Khoảng thời gian lịch không hợp lệ");
        }
        if (Duration.between(start, end).compareTo(Duration.ofDays(MAX_RANGE_DAYS)) > 0) {
            throw new DomainRuleViolationException("Khoảng thời gian lịch không được vượt quá 366 ngày");
        }
    }
}
