package com.glucoze.thesismanagement.thesis.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.thesis.service.ReportFileStorage;
import com.glucoze.thesismanagement.thesis.service.ThesisManagementService;
import com.glucoze.thesismanagement.thesis.service.ThesisQueryService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

class StudentReportControllerTest {

    @Test
    void streamsAuthorizedReportAsNonCacheableAttachment() {
        ThesisManagementService service = mock(ThesisManagementService.class);
        UserDetails user = mock(UserDetails.class);
        byte[] content = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
        when(user.getUsername()).thenReturn("student01");
        when(service.getStudentReportFile("student01", 7L)).thenReturn(
                new ReportFileStorage.StoredReportFile(content, "report.pdf", "application/pdf"));

        ResponseEntity<byte[]> response = new StudentReportController(service, mock(ThesisQueryService.class)).file(7L, user);

        assertThat(response.getBody()).isEqualTo(content);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .startsWith("attachment;")
                .contains("report.pdf");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }
}
