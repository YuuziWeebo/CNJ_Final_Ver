package com.glucoze.thesismanagement.council.dto;

import java.time.LocalDateTime;

public record CalendarScheduleRow(Long id, String thesisTitle, String studentName, String supervisorName,
                                  String councilName, String room, LocalDateTime start, LocalDateTime end) { }
