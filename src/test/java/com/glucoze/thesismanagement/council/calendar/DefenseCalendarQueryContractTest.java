package com.glucoze.thesismanagement.council.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DefenseCalendarQueryContractTest {
    @Test void roleQueriesApplyOverlapOwnershipOrderingAndDeduplicationInDatabase() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/glucoze/thesismanagement/council/repository/DefenseScheduleRepository.java"));
        assertThat(source).contains("l.startTime < :end and l.endTime > :start", "findLecturerCalendarRows",
                "userAccount.username = :username", "select distinct new", "order by l.startTime",
                "findStudentCalendarRows").doesNotContain("findAll().stream");
    }
}
