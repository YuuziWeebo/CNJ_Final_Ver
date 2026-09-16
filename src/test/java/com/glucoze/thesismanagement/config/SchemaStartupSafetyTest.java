package com.glucoze.thesismanagement.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SchemaStartupSafetyTest {

    @Test
    void applicationStartupDoesNotExecuteAdHocDestructiveDdl() throws IOException {
        Path javaRoot = Path.of("src/main/java");

        try (var sources = Files.walk(javaRoot)) {
            for (Path source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                String content = Files.readString(source).toUpperCase();
                assertThat(content)
                        .as(source.toString())
                        .doesNotContain("DROP COLUMN")
                        .doesNotContain("DROP TABLE");
            }
        }

        assertThat(javaRoot.resolve(
                "com/glucoze/thesismanagement/config/UserAccountSchemaMigration.java"))
                .doesNotExist();
    }
}
