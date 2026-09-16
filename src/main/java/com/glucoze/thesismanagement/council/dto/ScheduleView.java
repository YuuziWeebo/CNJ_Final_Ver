package com.glucoze.thesismanagement.council.dto;

import java.time.LocalDateTime;

public record ScheduleView(Long id, String thesisTitle, String councilName, String room,
                           LocalDateTime start, LocalDateTime end) {
}