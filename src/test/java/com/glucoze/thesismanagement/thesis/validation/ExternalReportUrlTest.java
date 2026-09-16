package com.glucoze.thesismanagement.thesis.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExternalReportUrlTest {

    @Test
    void acceptsAndNormalizesHttpsUrl() {
        assertThat(ExternalReportUrl.normalize("  HTTPS://example.com/a/../report.pdf  "))
                .isEqualTo("https://example.com/report.pdf");
    }

    @Test
    void rejectsUnsafeMalformedAndHostlessUrls() {
        List<String> invalidUrls = List.of(
                "javascript:alert(1)",
                "data:text/html,test",
                "file:///tmp/report.pdf",
                "//example.com/report.pdf",
                "https:///missing-host",
                "https://exa mple.com/report.pdf");

        invalidUrls.forEach(url -> assertThatThrownBy(() -> ExternalReportUrl.normalize(url))
                .as(url)
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("HTTPS"));
    }
}
