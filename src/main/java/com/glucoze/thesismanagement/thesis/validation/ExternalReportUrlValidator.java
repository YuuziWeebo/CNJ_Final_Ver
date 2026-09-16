package com.glucoze.thesismanagement.thesis.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ExternalReportUrlValidator implements ConstraintValidator<ValidExternalReportUrl, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.isBlank() || ExternalReportUrl.isValid(value);
    }
}
