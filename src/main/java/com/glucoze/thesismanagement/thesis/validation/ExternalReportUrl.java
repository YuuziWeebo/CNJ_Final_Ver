package com.glucoze.thesismanagement.thesis.validation;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import java.net.URI;
import java.net.URISyntaxException;

/** Canonical validation policy for externally hosted reports. */
public final class ExternalReportUrl {

    private ExternalReportUrl() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(value.trim()).normalize();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !uri.isAbsolute()
                    || uri.getHost() == null
                    || uri.getHost().isBlank()) {
                throw invalidUrl();
            }
            String normalized = uri.toASCIIString();
            return "https" + normalized.substring(uri.getScheme().length());
        } catch (URISyntaxException exception) {
            throw invalidUrl();
        }
    }

    public static boolean isValid(String value) {
        try {
            normalize(value);
            return true;
        } catch (DomainRuleViolationException exception) {
            return false;
        }
    }

    public static String normalizeOrNull(String value) {
        try {
            return normalize(value);
        } catch (DomainRuleViolationException exception) {
            return null;
        }
    }

    private static DomainRuleViolationException invalidUrl() {
        return new DomainRuleViolationException(
                "Liên kết báo cáo phải là URL HTTPS hợp lệ và có tên miền");
    }
}
