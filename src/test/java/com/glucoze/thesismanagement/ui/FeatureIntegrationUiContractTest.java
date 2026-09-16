package com.glucoze.thesismanagement.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FeatureIntegrationUiContractTest {

    private static final Path RESOURCES = Path.of("src/main/resources");

    @Test
    void notificationsHaveOneSharedHeaderEntryInsteadOfRoleSidebarDuplicates() throws IOException {
        String header = template("fragments/header.html");
        String sidebar = template("fragments/sidebar.html");

        assertThat(header)
                .contains("class=\"notification-bell\"")
                .contains("notificationUnreadCount > 99 ? '99+' : notificationUnreadCount")
                .contains("aria-label");
        assertThat(sidebar).doesNotContain("@{/notifications}");
    }

    @Test
    void adminNavigationUsesDistinctConsistentVietnameseLabels() throws IOException {
        assertThat(template("fragments/sidebar.html"))
                .contains("<span>Quản lý lịch</span>")
                .contains("<span>Lịch bảo vệ</span>")
                .contains("<span>Nhật ký hệ thống</span>")
                .contains("<span>Xuất dữ liệu</span>");
        assertThat(template("admin/audit-logs.html")).contains("<h1>Nhật ký hệ thống</h1>");
        assertThat(template("admin/exports.html")).contains("<h1>Xuất dữ liệu</h1>");
    }

    @Test
    void reportStatesShareOneSemanticBadgeMapping() throws IOException {
        String expected = "REVISION_REQUIRED' ? ' is-warning' : "
                + "(report.status.name() == 'SUBMITTED' ? ' is-info' : ' is-neutral')";

        assertThat(template("student/reports.html")).contains(expected);
        assertThat(template("lecturer/reports.html")).contains(expected);
    }

    @Test
    void calendarViewControlsExposeStateAndTodayWithoutUnsafeHtml() throws IOException {
        String calendar = template("schedules/calendar.html");
        String script = resource("static/js/defense-calendar.js");
        String styles = resource("static/css/panel/pages.css");

        assertThat(calendar)
                .contains("data-calendar-month-view aria-pressed=\"true\"")
                .contains("data-calendar-list-view aria-pressed=\"false\"");
        assertThat(script)
                .contains("const setView = view =>")
                .contains("heading.setAttribute('aria-current', 'date')")
                .doesNotContain("innerHTML");
        assertThat(styles)
                .contains(".calendar-day.is-today")
                .contains(".calendar-controls [aria-pressed='true']")
                .contains("var(--ui-primary)");
    }

    @Test
    void notificationBadgeSupportsThreeDigitCapWithoutFixedWidth() throws IOException {
        assertThat(resource("static/css/panel/components.css"))
                .contains(".notification-badge")
                .contains("white-space: nowrap")
                .contains("min-width: 1.15rem")
                .doesNotContain(".notification-badge { width:");
    }

    private String template(String relativePath) throws IOException {
        return resource("templates/" + relativePath);
    }

    private String resource(String relativePath) throws IOException {
        return Files.readString(RESOURCES.resolve(relativePath));
    }
}
