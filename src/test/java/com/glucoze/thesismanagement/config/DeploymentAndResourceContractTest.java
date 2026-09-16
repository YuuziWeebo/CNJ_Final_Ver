package com.glucoze.thesismanagement.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DeploymentAndResourceContractTest {

    private static final Path ROOT = Path.of("");
    private static final Path TEMPLATES = ROOT.resolve("src/main/resources/templates");
    private static final String BOOTSTRAP_CSS =
            "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css";
    private static final String BOOTSTRAP_JS =
            "https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js";

    @Test
    void versionedBootstrapAssetsAlwaysCarryPublishedIntegrityMetadata() throws IOException {
        try (var paths = Files.walk(TEMPLATES)) {
            for (Path template : paths.filter(path -> path.toString().endsWith(".html")).toList()) {
                String html = Files.readString(template);
                assertIntegrityWhenPresent(template, html, BOOTSTRAP_CSS,
                        "sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH");
                assertIntegrityWhenPresent(template, html, BOOTSTRAP_JS,
                        "sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz");
            }
        }
    }

    @Test
    void templatesDoNotReintroduceInlineScriptBlocks() throws IOException {
        try (var paths = Files.walk(TEMPLATES)) {
            for (Path template : paths.filter(path -> path.toString().endsWith(".html")).toList()) {
                String html = Files.readString(template);
                int scriptStart = html.indexOf("<script");
                while (scriptStart >= 0) {
                    int tagEnd = html.indexOf('>', scriptStart);
                    assertThat(tagEnd).as(template.toString()).isGreaterThan(scriptStart);
                    String openingTag = html.substring(scriptStart, tagEnd + 1);
                    assertThat(openingTag.contains(" src=\"") || openingTag.contains(" th:src=\""))
                            .as(template.toString())
                            .isTrue();
                    scriptStart = html.indexOf("<script", tagEnd + 1);
                }
            }
        }
    }

    @Test
    void loginBackgroundAndCursorAssetsUseSpringBootStaticLocations() throws IOException {
        Path staticImages = ROOT.resolve("src/main/resources/static/img");

        assertThat(staticImages.resolve("login_bg.png")).isRegularFile();
        assertThat(staticImages.resolve("cursor/01-normal-select_32-48-64.cur")).isRegularFile();
        assertThat(read("src/main/resources/static/css/forms.css"))
                .contains("url('../img/login_bg.png')");
        assertThat(read("src/main/resources/static/css/cursors.css"))
                .contains("url('../img/cursor/01-normal-select_32-48-64.cur')");
        assertThat(ROOT.resolve("src/main/resources/img")).doesNotExist();
    }

    @Test
    void dockerAndCiUseJava21WrapperAndExplicitUploadVolumeContract() throws IOException {
        String pom = read("pom.xml");
        String dockerfile = read("Dockerfile");
        String workflow = read(".github/workflows/main_quanlydoan-xxxxx.yml");

        assertThat(pom).contains("<java.version>21</java.version>");
        assertThat(dockerfile)
                .contains("eclipse-temurin:21-jdk")
                .contains("eclipse-temurin:21-jre")
                .contains("ENV APP_UPLOAD_DIR=/app/uploads")
                .contains("VOLUME [\"/app/uploads\"]")
                .contains("USER 10001")
                .contains("./mvnw --batch-mode clean package -DskipTests");
        assertThat(workflow)
                .contains("java-version: '21'")
                .contains("./mvnw --batch-mode clean verify")
                .doesNotContain("run: mvn ");
    }

    @Test
    void internalUserSummaryNamingUsesTheModuleViewConvention() throws IOException {
        assertThat(ROOT.resolve("src/main/java/com/glucoze/thesismanagement/user/dto/UserSummaryDto.java"))
                .doesNotExist();
        assertThat(read("src/main/java/com/glucoze/thesismanagement/user/dto/UserSummaryView.java"))
                .contains("record UserSummaryView");
        assertThat(read("src/main/java/com/glucoze/thesismanagement/user/service/UserManagementService.java"))
                .contains("List<UserSummaryView>")
                .doesNotContain("UserSummaryDto");
    }

    private void assertIntegrityWhenPresent(Path template, String html, String resource, String integrity) {
        if (html.contains(resource)) {
            assertThat(html).as(template.toString())
                    .contains("integrity=\"" + integrity + "\"")
                    .contains("crossorigin=\"anonymous\"");
        }
    }

    private String read(String path) throws IOException {
        return Files.readString(ROOT.resolve(path));
    }
}
