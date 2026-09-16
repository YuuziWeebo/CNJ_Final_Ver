package com.glucoze.thesismanagement.export.dto;

import java.time.LocalDateTime;

public record ScheduleExportRow(String thesisTitle, String studentName, String councilName, String room,
                                LocalDateTime startTime, LocalDateTime endTime,
                                String academicYear, String semester) { }
