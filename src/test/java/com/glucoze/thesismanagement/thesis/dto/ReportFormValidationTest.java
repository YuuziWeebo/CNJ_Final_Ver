package com.glucoze.thesismanagement.thesis.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class ReportFormValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsBlankOrHttpsAndRejectsUnsafeExternalUrl() {
        assertThat(validator.validate(new ReportForm())).isEmpty();
        assertThat(validator.validate(new ReportForm("https://example.com/report"))).isEmpty();
        assertThat(validator.validate(new ReportForm("javascript:alert(1)")))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("reportFile");
    }
}
