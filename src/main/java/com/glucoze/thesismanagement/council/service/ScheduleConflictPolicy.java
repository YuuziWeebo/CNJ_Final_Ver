package com.glucoze.thesismanagement.council.service;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import java.time.Duration;
import java.time.LocalDateTime;

public final class ScheduleConflictPolicy {

    public static final long BUFFER_MINUTES = 15;

    private ScheduleConflictPolicy() {
    }

    public static boolean overlapsWithBuffer(LocalDateTime firstStart, LocalDateTime firstEnd,
                                              LocalDateTime secondStart, LocalDateTime secondEnd) {
        LocalDateTime expandedFirstStart = firstStart.minusMinutes(BUFFER_MINUTES);
        LocalDateTime expandedFirstEnd = firstEnd.plusMinutes(BUFFER_MINUTES);
        return expandedFirstStart.isBefore(secondEnd) && expandedFirstEnd.isAfter(secondStart);
    }

    public static void requireValidInterval(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new DomainRuleViolationException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }
        if (Duration.between(start, end).isZero()) {
            throw new DomainRuleViolationException("Thời lượng lịch phải lớn hơn 0");
        }
    }
}
