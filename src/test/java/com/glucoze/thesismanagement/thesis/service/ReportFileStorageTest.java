package com.glucoze.thesismanagement.thesis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;

class ReportFileStorageTest {

    private static final String DOCX_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    @TempDir
    Path uploadDir;

    @Test
    void springCreatesStorageWithConfiguredUploadDirectory() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
            TestPropertyValues.of(
                    "app.upload-dir=" + uploadDir,
                    "app.upload.report-max-size=10MB")
                    .applyTo(context);
            context.register(ReportFileStorage.class);
            context.refresh();

            ReportFileStorage storage = context.getBean(ReportFileStorage.class);
            MockMultipartFile upload = new MockMultipartFile(
                    "file", "configured.pdf", "application/pdf",
                    "%PDF-1.7\nconfigured".getBytes(StandardCharsets.US_ASCII));
            String path = storage.store(upload);

            assertThat(uploadDir.resolve(path)).exists();
        }
    }

    @Test
    void storesAndLoadsPdfWithCanonicalExtension() {
        ReportFileStorage storage = new ReportFileStorage(uploadDir.toString());
        byte[] pdf = "%PDF-1.7\ncontent".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile upload = new MockMultipartFile("file", "unsafe.exe", "application/pdf", pdf);

        String path = storage.store(upload);
        ReportFileStorage.StoredReportFile stored = storage.load(path);

        assertThat(path).endsWith("_unsafe.pdf");
        assertThat(stored.filename()).isEqualTo("unsafe.pdf");
        assertThat(stored.contentType()).isEqualTo("application/pdf");
        assertThat(stored.content()).isEqualTo(pdf);
    }

    @Test
    void acceptsDocxOnlyWhenRequiredZipEntriesExist() throws Exception {
        ReportFileStorage storage = new ReportFileStorage(uploadDir.toString());
        byte[] docx = docxBytes();
        MockMultipartFile upload = new MockMultipartFile("file", "report.docx", DOCX_TYPE, docx);

        String path = storage.store(upload);

        assertThat(storage.load(path).content()).isEqualTo(docx);
    }

    @Test
    void rejectsSpoofedPdfContentType() {
        ReportFileStorage storage = new ReportFileStorage(uploadDir.toString());
        MockMultipartFile upload = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> storage.store(upload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong khop");
    }

    @Test
    void rejectsZipThatIsNotADocxDocument() throws Exception {
        ReportFileStorage storage = new ReportFileStorage(uploadDir.toString());
        MockMultipartFile upload = new MockMultipartFile(
                "file", "archive.docx", DOCX_TYPE, zipBytes(List.of("unrelated.txt")));

        assertThatThrownBy(() -> storage.store(upload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong khop");
    }

    @Test
    void rejectsPathTraversalWhenLoading() {
        ReportFileStorage storage = new ReportFileStorage(uploadDir.toString());

        assertThatThrownBy(() -> storage.load("reports/../../secret.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong hop le");
    }

    @Test
    void pathTraversalCannotReadOrDeleteFileOutsideReportsDirectory() throws Exception {
        ReportFileStorage storage = new ReportFileStorage(uploadDir.toString());
        Path outsideFile = uploadDir.resolve("outside.pdf");
        Files.writeString(outsideFile, "%PDF-1.7\noutside", StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> storage.load("reports/../outside.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong hop le");
        assertThatThrownBy(() -> storage.delete("reports/../outside.pdf"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khong hop le");
        assertThat(outsideFile).exists();
    }

    @Test
    void failedNewWriteLeavesExistingReportUntouchedAndRemovesTemporaryFile() throws Exception {
        Path reportsDir = Files.createDirectories(uploadDir.resolve("reports"));
        Path existingFile = reportsDir.resolve("existing.pdf");
        Files.writeString(existingFile, "%PDF-1.7\nexisting", StandardCharsets.US_ASCII);
        ReportFileStorage storage = new ReportFileStorage(uploadDir.toString(), (path, content) -> {
            throw new java.io.IOException("injected write failure");
        });
        MockMultipartFile upload = new MockMultipartFile(
                "file", "replacement.pdf", "application/pdf",
                "%PDF-1.7\nreplacement".getBytes(StandardCharsets.US_ASCII));

        assertThatThrownBy(() -> storage.store(upload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Khong the luu");

        assertThat(existingFile).exists();
        try (var files = Files.list(reportsDir)) {
            assertThat(files.map(path -> path.getFileName().toString()).toList())
                    .containsExactly("existing.pdf");
        }
    }

    @Test
    void reportSizeBoundaryUsesConfiguredApplicationLimit() {
        ReportFileStorage storage = new ReportFileStorage(
                uploadDir.toString(), DataSize.ofBytes(16), (path, content) -> Files.write(path, content));
        MockMultipartFile accepted = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "%PDF-12345678901".getBytes(StandardCharsets.US_ASCII));
        MockMultipartFile rejected = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "%PDF-123456789012".getBytes(StandardCharsets.US_ASCII));

        assertThat(storage.store(accepted)).startsWith("reports/");
        assertThatThrownBy(() -> storage.store(rejected))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("16 bytes");
    }

    private byte[] docxBytes() throws Exception {
        return zipBytes(List.of("[Content_Types].xml", "word/document.xml"));
    }

    private byte[] zipBytes(List<String> entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (String entryName : entries) {
                zip.putNextEntry(new ZipEntry(entryName));
                zip.write("content".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return output.toByteArray();
    }
}
