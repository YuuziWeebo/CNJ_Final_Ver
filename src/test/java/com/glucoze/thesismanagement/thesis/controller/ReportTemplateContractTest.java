package com.glucoze.thesismanagement.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ReportTemplateContractTest {

    @Test
    void reportFormExposesFileAndHttpsOptionsWithoutRequiringFile() throws IOException {
        String template = Files.readString(Path.of("src/main/resources/templates/student/report-form.html"));

        assertThat(template).contains("name=\"file\"").contains("th:field=\"*{reportFile}\"");
        assertThat(template).doesNotContain("name=\"file\" type=\"file\" required");
        assertThat(template).contains("Nếu có cả hai, file được ưu tiên");
        assertThat(template).contains("tối đa 10 MB");
    }

    @Test
    void externalLinksOpenInIsolatedTabButInternalDownloadsDoNot() throws IOException {
        for (String name : new String[] {"student/reports.html", "lecturer/reports.html"}) {
            String template = Files.readString(Path.of("src/main/resources/templates", name));
            assertThat(template).contains("target=\"_blank\" rel=\"noopener noreferrer\"");
            assertThat(template).contains("#strings.startsWith(report.file, 'https://')");
            assertThat(template).doesNotContain("file/{id}(id=${report.id})}\" target=\"_blank\"");
        }
    }
}
