package com.glucoze.thesismanagement.council.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ScheduleConflictPolicyTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 20, 9, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 20, 10, 0);

    @Test
    void rejectsPartiallyOverlappingSession() {
        assertThat(ScheduleConflictPolicy.overlapsWithBuffer(
                START, END, LocalDateTime.of(2026, 8, 20, 9, 45), LocalDateTime.of(2026, 8, 20, 10, 30)))
                .isTrue();
    }

    @Test
    void rejectsIdenticalStartTime() {
        assertThat(ScheduleConflictPolicy.overlapsWithBuffer(
                START, END, START, LocalDateTime.of(2026, 8, 20, 9, 30)))
                .isTrue();
    }

    @Test
    void rejectsGapShorterThanRequiredBuffer() {
        assertThat(ScheduleConflictPolicy.overlapsWithBuffer(
                START, END, LocalDateTime.of(2026, 8, 20, 10, 14), LocalDateTime.of(2026, 8, 20, 11, 0)))
                .isTrue();
    }

    @Test
    void allowsExactlyRequiredBuffer() {
        assertThat(ScheduleConflictPolicy.overlapsWithBuffer(
                START, END, LocalDateTime.of(2026, 8, 20, 10, 15), LocalDateTime.of(2026, 8, 20, 11, 0)))
                .isFalse();
    }

    @Test
    void rejectsZeroDurationWithTheExistingIntervalError() {
        assertThatThrownBy(() -> ScheduleConflictPolicy.requireValidInterval(START, START))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessage("Thời gian bắt đầu phải trước thời gian kết thúc");
    }

    @Test
    void rejectsNegativeDurationWithTheExistingIntervalError() {
        assertThatThrownBy(() -> ScheduleConflictPolicy.requireValidInterval(END, START))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessage("Thời gian bắt đầu phải trước thời gian kết thúc");
    }

    @Test
    void acceptsPositiveDuration() {
        assertThatCode(() -> ScheduleConflictPolicy.requireValidInterval(START, END))
                .doesNotThrowAnyException();
    }
}
