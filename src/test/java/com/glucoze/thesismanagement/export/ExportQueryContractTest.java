package com.glucoze.thesismanagement.export;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ExportQueryContractTest {
    @Test void resultPredicateAndScheduleOrderingRemainInDatabaseQueries() throws Exception {
        String results = Files.readString(Path.of("src/main/java/com/glucoze/thesismanagement/grading/repository/ResultRepository.java"));
        String schedules = Files.readString(Path.of("src/main/java/com/glucoze/thesismanagement/council/repository/DefenseScheduleRepository.java"));
        assertThat(results).contains("where k.published = true", "order by k.defenseSchedule.startTime")
                .doesNotContain("findAllResults");
        assertThat(schedules).contains("order by l.startTime", "new com.glucoze.thesismanagement.export.dto.ScheduleExportRow");
    }

    @Test void exportImplementationUsesExplicitRowsWithoutReflectionOrEntityFindAll() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/glucoze/thesismanagement/export/ManagementExportService.java"));
        assertThat(service).contains("EXPORT_LIMIT = 10_000", "safeSpreadsheetText")
                .doesNotContain("findAll()", "getDeclaredFields", "java.lang.reflect", "NotificationService");
    }
}
