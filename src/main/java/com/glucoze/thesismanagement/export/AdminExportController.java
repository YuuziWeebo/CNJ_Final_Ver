package com.glucoze.thesismanagement.export;

import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/exports")
public class AdminExportController {
    static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final ManagementExportService service;

    public AdminExportController(ManagementExportService service) { this.service = service; }

    @GetMapping
    public String index(Model model) { return "admin/exports"; }

    @GetMapping("/students.xlsx")
    public ResponseEntity<byte[]> students() { return download(service.studentsExcel(), XLSX, "danh-sach-sinh-vien.xlsx"); }

    @GetMapping("/lecturers.xlsx")
    public ResponseEntity<byte[]> lecturers() { return download(service.lecturersExcel(), XLSX, "danh-sach-giang-vien.xlsx"); }

    @GetMapping("/theses.xlsx")
    public ResponseEntity<byte[]> theses() { return download(service.thesesExcel(), XLSX, "danh-sach-de-tai.xlsx"); }

    @GetMapping("/councils.xlsx")
    public ResponseEntity<byte[]> councils() { return download(service.councilsExcel(), XLSX, "danh-sach-hoi-dong.xlsx"); }

    @GetMapping("/schedules.xlsx")
    public ResponseEntity<byte[]> schedulesExcel(@RequestParam(required = false) String academicYear,
                                                  @RequestParam(required = false) String semester) {
        return download(service.schedulesExcel(academicYear, semester), XLSX,
                filteredFilename("lich-bao-ve", academicYear, semester, "xlsx"));
    }

    @GetMapping("/results.xlsx")
    public ResponseEntity<byte[]> resultsExcel(@RequestParam(required = false) String academicYear,
                                                @RequestParam(required = false) String semester) {
        return download(service.resultsExcel(academicYear, semester), XLSX,
                filteredFilename("ket-qua-bao-ve", academicYear, semester, "xlsx"));
    }

    @GetMapping("/schedules.pdf")
    public ResponseEntity<byte[]> schedulesPdf(@RequestParam(required = false) String academicYear,
                                                @RequestParam(required = false) String semester) {
        return download(service.schedulesPdf(academicYear, semester), MediaType.APPLICATION_PDF,
                filteredFilename("lich-bao-ve", academicYear, semester, "pdf"));
    }

    @GetMapping("/results.pdf")
    public ResponseEntity<byte[]> resultsPdf(@RequestParam(required = false) String academicYear,
                                              @RequestParam(required = false) String semester) {
        return download(service.resultsPdf(academicYear, semester), MediaType.APPLICATION_PDF,
                filteredFilename("ket-qua-bao-ve", academicYear, semester, "pdf"));
    }

    private ResponseEntity<byte[]> download(byte[] content, MediaType type, String filename) {
        String disposition = ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString();
        return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentLength(content.length).body(content);
    }

    static String filteredFilename(String prefix, String year, String semester, String extension) {
        return prefix + suffix(year) + suffix(semester) + "." + extension;
    }

    private static String suffix(String value) {
        if (value == null || value.isBlank()) return "";
        String safe = value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-").replaceAll("^-|-$", "");
        return safe.isEmpty() ? "" : "-" + safe.substring(0, Math.min(safe.length(), 24));
    }
}
