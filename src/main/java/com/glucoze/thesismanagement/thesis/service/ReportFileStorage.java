package com.glucoze.thesismanagement.thesis.service;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.http.MediaType;
import org.springframework.util.unit.DataSize;

/**
 * Luu/xoa file bao cao tren filesystem duoi thu muc uploads/reports/.
 */
@Service
public class ReportFileStorage {

    static final DataSize DEFAULT_MAX_FILE_SIZE = DataSize.ofMegabytes(10);
    static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};
    private static final String DOCX_CONTENT_TYPES_ENTRY = "[Content_Types].xml";
    private static final String DOCX_DOCUMENT_ENTRY = "word/document.xml";

    private final Path reportsDir;
    private final FileWriter fileWriter;
    private final long maxFileSize;

    @Autowired
    public ReportFileStorage(@Value("${app.upload-dir:uploads}") String uploadDir,
                             @Value("${app.upload.report-max-size:10MB}") DataSize maxFileSize) {
        this(uploadDir, maxFileSize, (path, content) -> Files.write(path, content));
    }

    ReportFileStorage(String uploadDir) {
        this(uploadDir, DEFAULT_MAX_FILE_SIZE, (path, content) -> Files.write(path, content));
    }

    ReportFileStorage(String uploadDir, FileWriter fileWriter) {
        this(uploadDir, DEFAULT_MAX_FILE_SIZE, fileWriter);
    }

    ReportFileStorage(String uploadDir, DataSize maxFileSize, FileWriter fileWriter) {
        this.reportsDir = Paths.get(uploadDir, "reports").toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize.toBytes();
        this.fileWriter = fileWriter;
    }

    /** Validate va luu file. Tra ve relative path dang "reports/{uuid}_{name}". */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainRuleViolationException("File khong duoc de trong");
        }
        if (file.getSize() > maxFileSize) {
            throw new DomainRuleViolationException(
                    "File báo cáo không được vượt quá " + displaySize(maxFileSize));
        }
        String contentType = file.getContentType();
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new DomainRuleViolationException("Chi ho tro file PDF hoac DOCX");
        }
        Path temporaryFile = null;
        try {
            byte[] content = file.getBytes();
            validateContent(contentType, content);
            Files.createDirectories(reportsDir);
            temporaryFile = Files.createTempFile(reportsDir, ".report-upload-", ".tmp");
            fileWriter.write(temporaryFile, content);
            String safeName = UUID.randomUUID() + "_" + canonicalFilename(file.getOriginalFilename(), contentType);
            moveIntoPlace(temporaryFile, reportsDir.resolve(safeName));
            temporaryFile = null;
            return "reports/" + safeName;
        } catch (IOException e) {
            throw new DomainRuleViolationException("Khong the luu file bao cao", e);
        } finally {
            deleteTemporaryFile(temporaryFile);
        }
    }

    public StoredReportFile load(String relativePath) {
        Path file = resolveReportPath(relativePath);
        try {
            byte[] content = Files.readAllBytes(file);
            String filename = file.getFileName().toString();
            String contentType = filename.toLowerCase().endsWith(".docx")
                    ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    : MediaType.APPLICATION_PDF_VALUE;
            validateContent(contentType, content);
            return new StoredReportFile(content, originalFilename(filename), contentType);
        } catch (IOException exception) {
            throw new DomainRuleViolationException("Khong the doc file bao cao", exception);
        }
    }

    /** Xoa file tren disk. Khong throw neu file hop le nhung khong ton tai. */
    public void delete(String relativePath) {
        if (relativePath == null || !relativePath.startsWith("reports/")) return;
        try {
            Files.deleteIfExists(resolveReportPath(relativePath));
        } catch (IOException ignored) {}
    }

    private String sanitize(String name) {
        if (name == null) return "file";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String canonicalFilename(String originalName, String contentType) {
        String sanitized = sanitize(originalName);
        int extensionIndex = sanitized.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? sanitized.substring(0, extensionIndex) : sanitized;
        if (baseName.isBlank()) {
            baseName = "file";
        }
        String extension = MediaType.APPLICATION_PDF_VALUE.equals(contentType) ? ".pdf" : ".docx";
        return baseName + extension;
    }

    private Path resolveReportPath(String relativePath) {
        if (relativePath == null || !relativePath.startsWith("reports/")) {
            throw new DomainRuleViolationException("Duong dan file bao cao khong hop le");
        }
        Path file = reportsDir.resolve(relativePath.substring("reports/".length())).normalize();
        if (!file.startsWith(reportsDir)) {
            throw new DomainRuleViolationException("Duong dan file bao cao khong hop le");
        }
        return file;
    }

    private void validateContent(String contentType, byte[] content) {
        boolean valid = MediaType.APPLICATION_PDF_VALUE.equals(contentType)
                ? startsWith(content, PDF_SIGNATURE)
                : isDocx(content);
        if (!valid) {
            throw new DomainRuleViolationException("Noi dung file khong khop dinh dang PDF hoac DOCX");
        }
    }

    private boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (content[index] != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isDocx(byte[] content) {
        boolean hasContentTypes = false;
        boolean hasDocument = false;
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                hasContentTypes |= DOCX_CONTENT_TYPES_ENTRY.equals(entry.getName());
                hasDocument |= DOCX_DOCUMENT_ENTRY.equals(entry.getName());
                if (hasContentTypes && hasDocument) {
                    return true;
                }
            }
            return false;
        } catch (IOException exception) {
            return false;
        }
    }

    private String originalFilename(String storedFilename) {
        int separator = storedFilename.indexOf('_');
        return separator >= 0 && separator + 1 < storedFilename.length()
                ? storedFilename.substring(separator + 1)
                : storedFilename;
    }

    private void moveIntoPlace(Path temporaryFile, Path finalFile) throws IOException {
        try {
            Files.move(temporaryFile, finalFile, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, finalFile);
        }
    }

    private void deleteTemporaryFile(Path temporaryFile) {
        if (temporaryFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException ignored) {
            // The original write exception remains the actionable failure.
        }
    }

    private String displaySize(long bytes) {
        if (bytes % (1024 * 1024) == 0) {
            return bytes / (1024 * 1024) + " MB";
        }
        return bytes >= 1024 ? bytes / 1024 + " KB" : bytes + " bytes";
    }

    @FunctionalInterface
    interface FileWriter {
        void write(Path path, byte[] content) throws IOException;
    }

    public record StoredReportFile(byte[] content, String filename, String contentType) {
    }
}
