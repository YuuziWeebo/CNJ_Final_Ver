package com.glucoze.thesismanagement.council.dto;

import java.time.LocalDateTime;

public record CalendarEventView(Long id, String title, LocalDateTime start, LocalDateTime end,
                                String room, String councilName, String supervisorName,
                                String studentName, String detailUrl) { }
