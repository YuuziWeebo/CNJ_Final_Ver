package com.glucoze.thesismanagement.council.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CouncilFormValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresAllCouncilRoles() {
        CouncilForm form = new CouncilForm();
        form.setName("Hội đồng 1");
        form.setAcademicYear("2026-2027");
        form.setSemester("1");

        assertThat(validator.validate(form, CouncilForm.Create.class))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("chairId", "secretaryId", "memberId");

        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void scheduleConstraintsMatchRequiredEntityColumns() {
        ScheduleForm form = new ScheduleForm();

        assertThat(validator.validate(form))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("thesisId", "councilId", "room", "startTime", "endTime");

        form.setThesisId(1L);
        form.setCouncilId(2L);
        form.setRoom("A101");
        form.setStartTime(LocalDateTime.of(2026, 9, 10, 8, 0));
        form.setEndTime(LocalDateTime.of(2026, 9, 10, 9, 0));
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void rejectsRoomLongerThanEntityColumn() {
        ScheduleForm form = new ScheduleForm();
        form.setThesisId(1L);
        form.setCouncilId(2L);
        form.setRoom("A".repeat(151));
        form.setStartTime(LocalDateTime.now());
        form.setEndTime(LocalDateTime.now().plusHours(1));

        assertThat(validator.validate(form))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("room");
    }
}
