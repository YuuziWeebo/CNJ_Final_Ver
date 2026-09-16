package com.glucoze.thesismanagement.home.dto;

import com.glucoze.thesismanagement.council.dto.ScheduleView;
import java.util.List;

public record AdminDashboardView(long studentCount, long lecturerCount, long thesisCount,
                                 long pendingRegistrationCount, long pendingReportCount,
                                 long councilCount, long scheduleCount, long publishedResultCount,
                                 List<ScheduleView> upcomingSchedules) {
}
