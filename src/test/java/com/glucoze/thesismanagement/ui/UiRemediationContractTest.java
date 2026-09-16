package com.glucoze.thesismanagement.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class UiRemediationContractTest {

    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void destructiveAndFinalizingActionsUseSpecificConfirmationHooks() throws IOException {
        assertConfirmation("lecturer/theses.html", "Xóa đề tài");
        assertConfirmation("admin/councils.html", "Xóa hội đồng");
        assertConfirmation("admin/council-members.html", "Gỡ ${member.fullName}");
        assertConfirmation("admin/users.html", "Xóa tài khoản");
        assertConfirmation("lecturer/registrations.html", "Từ chối đăng ký");
        assertConfirmation("lecturer/grading-form.html", "Công bố kết quả");
        assertConfirmation("lecturer/reports.html", "Duyệt báo cáo");
        assertThat(read("admin/user-edit-form.html"))
                .contains("data-confirm-disabled-field=\"enabled\"")
                .contains("Khóa tài khoản này?")
                .contains("@{/js/confirm-actions.js}");

        String ordinaryFilter = read("student/theses.html");
        assertThat(ordinaryFilter).contains("method=\"get\"").doesNotContain("data-confirm");
    }

    @Test
    void auditedTemplatesDoNotExposeKnownFakeControls() throws IOException {
        assertThat(read("fragments/header.html"))
                .doesNotContain("type=\"search\"")
                .doesNotContain("dashboard-search");
        for (String template : List.of("admin/users.html", "admin/councils.html", "admin/schedules.html")) {
            assertThat(read(template)).doesNotContain("admin-table-filter").doesNotContain("href=\"#\"");
        }
        assertThat(read("admin/councils.html")).doesNotContain("Ổn định").doesNotContain("Đã lập");
        assertThat(read("admin/schedules.html")).doesNotContain("Ổn định");
        assertThat(read("home.html"))
                .doesNotContain("systemHealth")
                .doesNotContain("Tình trạng hệ thống")
                .doesNotContain("href=\"#");
    }

    @Test
    void everyProfileUsesOneSharedCropperWithoutInlineImplementation() throws IOException {
        for (String role : List.of("admin", "student", "lecturer")) {
            String template = read(role + "/profile.html");
            assertThat(template)
                    .contains("@{/js/avatar-cropper.js}")
                    .contains("data-avatar-cropper")
                    .contains("data-avatar-input")
                    .contains("data-crop-confirm")
                    .doesNotContain("new FileReader()")
                    .doesNotContain("function onConfirm");
        }
    }

    private void assertConfirmation(String template, String messageFragment) throws IOException {
        assertThat(read(template))
                .contains("data-confirm")
                .contains("@{/js/confirm-actions.js}")
                .contains(messageFragment);
    }

    private String read(String relativePath) throws IOException {
        return Files.readString(TEMPLATES.resolve(relativePath));
    }
}
