package com.glucoze.thesismanagement.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CssThemeContractTest {

    private static final Path CSS = Path.of("src/main/resources/static/css");
    private static final Path TEMPLATES = Path.of("src/main/resources/templates");

    @Test
    void sharedEntrypointKeepsThemeLayersInOneExplicitOrder() throws IOException {
        assertThat(readCss("app.css")).containsSubsequence(
                "@import url('_tokens.css?v=20260910.4');",
                "@import url('base.css?v=20260910.4');",
                "@import url('components.css?v=20260910.4');",
                "@import url('forms.css?v=20260910.4');",
                "@import url('dashboards.css?v=20260910.4');",
                "@import url('responsive.css?v=20260910.4');");
        assertThat(CSS.resolve("student-dashboard.css")).doesNotExist();
    }

    @Test
    void representativeRoleAndFeaturePagesUseTheSharedStylesheet() throws IOException {
        for (String template : List.of(
                "login.html",
                "home.html",
                "admin/users.html",
                "admin/councils.html",
                "admin/schedules.html",
                "lecturer/theses.html",
                "lecturer/registrations.html",
                "lecturer/grading.html",
                "lecturer/reports.html",
                "student/theses.html",
                "student/grading.html",
                "student/reports.html",
                "student/profile.html")) {
            assertThat(readTemplate(template)).as(template).contains("@{/css/app.css(v='20260910.4')}");
        }
    }

    @Test
    void themeAssetsUseTheCurrentCacheVersionOnEveryStyledPage() throws IOException {
        try (var paths = Files.walk(TEMPLATES)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".html")).toList()) {
                String template = Files.readString(path);
                if (!template.contains("app.css")) continue;

                assertThat(template)
                        .as(TEMPLATES.relativize(path).toString())
                        .contains("@{/css/app.css(v='20260910.4')}")
                        .contains("@{/js/theme.js(v='20260910.4')}")
                        .doesNotContain("@{/css/app.css}")
                        .doesNotContain("@{/js/theme.js}");
            }
        }

        assertThat(readCss("app.css"))
                .contains("@import url('theme-animations.css?v=20260910.4');")
                .contains("@import url('forms.css?v=20260910.4');")
                .contains("@import url('panel-system.css?v=20260910.4');");
        assertThat(readCss("panel-system.css"))
                .contains("@import url('panel/theme.css?v=20260910.4');");
    }

    @Test
    void semanticTokensBackSharedComponentsAndRoleDashboards() throws IOException {
        assertThat(readCss("_tokens.css"))
                .contains("--tm-font-family:")
                .contains("--tm-success:")
                .contains("--tm-warning:")
                .contains("--tm-danger:")
                .contains("--tm-radius-card:")
                .contains("--tm-shadow-panel:")
                .contains("--tm-role-student:")
                .contains("--tm-role-lecturer:")
                .contains("--tm-role-admin:");
        assertThat(readCss("components.css"))
                .contains(".status-badge.is-success")
                .contains("var(--tm-success-soft)")
                .contains(".avatar-crop-wrapper");
        assertThat(readCss("dashboards.css"))
                .contains("var(--tm-role-student)")
                .contains("var(--tm-role-lecturer)")
                .contains("var(--tm-role-admin)")
                .doesNotContain("student-search")
                .doesNotContain("dashboard-search")
                .doesNotContain("admin-table-filter");
    }

    @Test
    void themeTransitionUsesOneRandomSpreadAndRespectsReducedMotion() throws IOException {
        String animationCss = readCss("theme-animations.css");
        String themeScript = Files.readString(Path.of("src/main/resources/static/js/theme.js"));

        assertThat(animationCss)
                .contains("animation-duration: 720ms;")
                .contains("data-theme-transition='random-spread'")
                .contains("@keyframes theme-random-spread")
                .contains("42% { clip-path:")
                .contains("58% { clip-path:")
                .contains("@media (prefers-reduced-motion: reduce)")
                .doesNotContain("theme-left-to-right")
                .doesNotContain("theme-top-left")
                .doesNotContain("theme-horizontal-pause");

        assertThat(themeScript)
                .contains("const setRandomSpreadGeometry = () =>")
                .contains("0.1 + Math.random() * 0.8")
                .contains("root.dataset.themeTransition = 'random-spread'")
                .contains("transition.finished.then(cleanup, cleanup);")
                .contains("transitionRunning = false;")
                .contains("delete root.dataset.themeTransition;");

        assertThat(themeScript)
                .containsPattern("if\\s*\\(reduceMotion\\.matches\\s*\\|\\|\\s*"
                        + "typeof document\\.startViewTransition !== 'function'\\)\\s*\\{\\s*"
                        + "applyTheme\\(nextTheme\\);\\s*return;\\s*}")
                .containsPattern("catch\\s*\\([^)]*\\)\\s*\\{[^}]*"
                        + "applyTheme\\(nextTheme\\);\\s*cleanup\\(\\);\\s*}");

        assertThat(countOccurrences(themeScript, "root.dataset.themeTransition = 'random-spread'"))
                .isEqualTo(1);
        assertThat(countOccurrences(animationCss, "animation-name: theme-random-spread"))
                .isEqualTo(1);
    }

    @Test
    void loginBackgroundRemainsVisibleInBothThemes() throws IOException {
        assertThat(readCss("forms.css"))
                .contains("linear-gradient(135deg, rgba(8, 26, 42, 0.48), rgba(12, 55, 83, 0.18))")
                .contains("url('../img/login_bg.png') center / cover no-repeat fixed");
        assertThat(readCss("panel/theme.css"))
                .contains(":root[data-theme='dark'] body:not(.login-page)")
                .doesNotContain(":root[data-theme='dark'] body {");
    }

    @Test
    void auditedTemplatesUseSharedStatusStylesAndNoStaticInlineCss() throws IOException {
        for (String template : List.of(
                "lecturer/registrations.html",
                "lecturer/reports.html",
                "student/theses.html",
                "student/reports.html")) {
            assertThat(readTemplate(template)).as(template).contains("status-badge");
        }

        try (var paths = Files.walk(TEMPLATES)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".html")).toList()) {
                assertThat(Files.readString(path))
                        .as(TEMPLATES.relativize(path).toString())
                        .doesNotContain("<style")
                        .doesNotContain(" style=\"");
            }
        }
    }

    @Test
    void roleDashboardsReuseSharedResponsiveAccessibleComponents() throws IOException {
        String home = readTemplate("home.html");
        assertThat(home)
                .contains("class=\"stat-grid\"")
                .contains("class=\"dashboard-section\"")
                .contains("class=\"progress-steps\"")
                .doesNotContain("style=\"");
        assertThat(readCss("panel/pages.css"))
                .contains("var(--ui-surface)")
                .contains("var(--ui-border)")
                .contains("@media (max-width: 1199.98px)")
                .contains("@media (max-width: 575.98px)")
                .contains("@media (prefers-reduced-motion: reduce)");
    }

    @Test
    void notificationsReuseSharedHeaderTokensAndAccessibleStates() throws IOException {
        assertThat(readTemplate("fragments/header.html"))
                .contains("class=\"notification-bell\"")
                .contains("notificationUnreadCount > 0")
                .contains("aria-label");
        assertThat(readTemplate("notifications/list.html"))
                .contains("panel-empty")
                .contains("is-unread")
                .contains("@{/notifications/read-all}")
                .contains("_csrf");
        assertThat(readCss("panel/components.css"))
                .contains(".notification-bell:focus-visible")
                .contains("var(--ui-primary-soft)")
                .doesNotContain("notification-badge { animation");
    }

    @Test
    void exportHubReusesSharedAccessibleResponsiveComponents() throws IOException {
        assertThat(readTemplate("admin/exports.html"))
                .contains("panel-icon-action")
                .contains("panel-filter-field")
                .contains("<label for=\"academicYear\"")
                .doesNotContain("onclick=", "style=\"");
        assertThat(readCss("panel/pages.css"))
                .contains(".export-link-grid")
                .contains("@media (max-width: 575.98px)")
                .doesNotContain(".export-link-grid { animation");
    }

    private String readCss(String relativePath) throws IOException {
        return Files.readString(CSS.resolve(relativePath));
    }

    private String readTemplate(String relativePath) throws IOException {
        return Files.readString(TEMPLATES.resolve(relativePath));
    }

    private int countOccurrences(String source, String expected) {
        return source.split(java.util.regex.Pattern.quote(expected), -1).length - 1;
    }
}
