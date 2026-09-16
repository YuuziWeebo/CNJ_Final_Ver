package com.glucoze.thesismanagement.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.glucoze.thesismanagement.council.repository.CouncilRepository;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.export.dto.ResultExportRow;
import com.glucoze.thesismanagement.export.dto.ScheduleExportRow;
import com.glucoze.thesismanagement.export.dto.StudentExportRow;
import com.glucoze.thesismanagement.grading.repository.ResultRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagementExportServiceTest {
    @Mock StudentRepository students; @Mock LecturerRepository lecturers; @Mock ThesisRepository theses;
    @Mock CouncilRepository councils; @Mock DefenseScheduleRepository schedules; @Mock ResultRepository results;
    ManagementExportService service;

    @BeforeEach void setUp() { service = new ManagementExportService(students, lecturers, theses, councils, schedules, results); }

    @Test void studentWorkbookIsValidUnicodeAndFormulaSafe() throws Exception {
        when(students.findExportRows(any())).thenReturn(List.of(
                new StudentExportRow("SV01", "Nguyễn Văn An", "=HYPERLINK(\"https://evil\")")));
        byte[] bytes = service.studentsExcel();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("SinhVien");
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Mã sinh viên");
            assertThat(sheet.getLastRowNum()).isEqualTo(1);
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Nguyễn Văn An");
            assertThat(sheet.getRow(1).getCell(2).getCellType()).isEqualTo(CellType.STRING);
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).startsWith("'=");
        }
        assertThat(new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1))
                .doesNotContain("$2a$", "passwordHash", "secret-token", "avatar_data");
    }

    @Test void resultWorkbookStoresScoreAsNumber() throws Exception {
        when(results.findPublishedExportRows(any(), any(), any(), any())).thenReturn(List.of(
                new ResultExportRow("Trần Thị Hương", "Đồ án quản lý khóa luận", "GVHD", "Hội đồng",
                        new BigDecimal("8.5"), LocalDateTime.of(2026, 5, 2, 8, 0), "2025-2026", "HK2")));
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(service.resultsExcel(null, null)))) {
            assertThat(workbook.getSheet("KetQua").getRow(1).getCell(4).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(workbook.getSheet("KetQua").getRow(1).getCell(4).getNumericCellValue()).isEqualTo(8.5);
            assertThat(workbook.getSheet("KetQua").getRow(1).getCell(5).getCellType()).isEqualTo(CellType.NUMERIC);
        }
    }

    @Test void schedulePdfIsParseableAndKeepsVietnameseUnicode() throws Exception {
        when(schedules.findExportRows(any(), any(), any(), any())).thenReturn(List.of(
                new ScheduleExportRow("Đồ án quản lý khóa luận", "Nguyễn Văn An", "Hội đồng 1", "P101",
                        LocalDateTime.of(2026, 5, 2, 8, 0), LocalDateTime.of(2026, 5, 2, 9, 0), "2025-2026", "HK2")));
        byte[] bytes = service.schedulesPdf("2025-2026", "HK2");
        assertThat(new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        try (var document = Loader.loadPDF(bytes)) {
            assertThat(new PDFTextStripper().getText(document)).contains("Đồ án quản lý khóa luận", "Nguyễn Văn An");
        }
    }

    @Test void base64FontResourceDecodesAndLoadsIntoPdfBox() throws Exception {
        try (PDDocument document = new PDDocument()) {
            var font = ManagementExportService.loadUnicodeFont(document, "/fonts/DejaVuSans.ttf.b64");
            assertThat(font.getName()).containsIgnoringCase("DejaVuSans");
        }
    }

    @Test void malformedOrMissingBase64FontFailsClearly() {
        try (PDDocument document = new PDDocument()) {
            assertThatThrownBy(() -> ManagementExportService.loadUnicodeFont(document, "/fonts/invalid-font.b64"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Base64");
            assertThatThrownBy(() -> ManagementExportService.loadUnicodeFont(document, "/fonts/missing-font.b64"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Không tìm thấy tài nguyên font PDF Unicode");
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    @Test void emptyExportsRemainDeterministic() throws Exception {
        when(students.findExportRows(any())).thenReturn(List.of());
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(service.studentsExcel()))) {
            assertThat(workbook.getSheet("SinhVien").getLastRowNum()).isZero();
        }
        when(results.findPublishedExportRows(any(), any(), any(), any())).thenReturn(List.of());
        try (var document = Loader.loadPDF(service.resultsPdf(null, null))) {
            assertThat(new PDFTextStripper().getText(document)).contains("Không có dữ liệu phù hợp");
        }
    }
}
