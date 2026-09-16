package com.glucoze.thesismanagement.council.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DefenseCalendarUiContractTest {
    @Test void calendarIsSelfHostedReadOnlyResponsiveAndTokenBased() throws Exception {
        String html = Files.readString(Path.of("src/main/resources/templates/schedules/calendar.html"));
        String js = Files.readString(Path.of("src/main/resources/static/js/defense-calendar.js"));
        String css = Files.readString(Path.of("src/main/resources/static/css/panel/pages.css"));
        assertThat(html).contains("@{/js/defense-calendar.js", "data-calendar-loading", "data-calendar-error",
                "data-calendar-empty", "aria-label").doesNotContain("fullcalendar", "cdn.jsdelivr.net/npm/fullcalendar");
        assertThat(js).contains("textContent", "encodeURIComponent", "max-width: 767.98px")
                .doesNotContain("eventDrop", "eventResize", "draggable", "innerHTML", "WebSocket");
        assertThat(css).contains("var(--ui-surface)", "var(--ui-border)", "prefers-reduced-motion")
                .doesNotContain(".calendar-event { animation");
    }
}
