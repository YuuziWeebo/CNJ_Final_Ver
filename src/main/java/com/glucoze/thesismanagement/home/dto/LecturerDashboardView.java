package com.glucoze.thesismanagement.home.dto;

import com.glucoze.thesismanagement.council.dto.ScheduleView;
import java.util.List;

public record LecturerDashboardView(long supervisedThesisCount, long pendingRegistrationCount,
                                    long pendingReportCount, long incompleteGradingTaskCount,
                                    List<ScheduleView> upcomingSchedules) {
}
