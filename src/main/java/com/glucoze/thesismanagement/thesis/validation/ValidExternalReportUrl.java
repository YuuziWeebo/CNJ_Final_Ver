package com.glucoze.thesismanagement.thesis.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExternalReportUrlValidator.class)
public @interface ValidExternalReportUrl {

    String message() default "Liên kết báo cáo phải là URL HTTPS hợp lệ và có tên miền";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
