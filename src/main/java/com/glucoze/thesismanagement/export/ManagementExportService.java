package com.glucoze.thesismanagement.export;

import com.glucoze.thesismanagement.common.enums.RegistrationStatus;
import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.council.repository.CouncilRepository;
import com.glucoze.thesismanagement.council.repository.DefenseScheduleRepository;
import com.glucoze.thesismanagement.export.dto.CouncilExportRow;
import com.glucoze.thesismanagement.export.dto.LecturerExportRow;
import com.glucoze.thesismanagement.export.dto.ResultExportRow;
import com.glucoze.thesismanagement.export.dto.ScheduleExportRow;
import com.glucoze.thesismanagement.export.dto.StudentExportRow;
import com.glucoze.thesismanagement.export.dto.ThesisExportRow;
import com.glucoze.thesismanagement.grading.repository.ResultRepository;
import com.glucoze.thesismanagement.thesis.repository.ThesisRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ManagementExportService {
    private static final int EXPORT_LIMIT = 10_000;
    private static final PageRequest EXPORT_PAGE = PageRequest.of(0, EXPORT_LIMIT);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final StudentRepository students;
    private final LecturerRepository lecturers;
    private final ThesisRepository theses;
    private final CouncilRepository councils;
    private final DefenseScheduleRepository schedules;
    private final ResultRepository results;

    public ManagementExportService(StudentRepository students, LecturerRepository lecturers,
                                   ThesisRepository theses, CouncilRepository councils,
                                   DefenseScheduleRepository schedules, ResultRepository results) {
        this.students = students; this.lecturers = lecturers; this.theses = theses;
        this.councils = councils; this.schedules = schedules; this.results = results;
    }

    public byte[] studentsExcel() {
        return workbook("SinhVien", List.of("Mã sinh viên", "Họ tên", "Lớp"),
                students.findExportRows(EXPORT_PAGE), r -> List.of(r.studentCode(), r.fullName(), nullable(r.className())));
    }

    public byte[] lecturersExcel() {
        return workbook("GiangVien", List.of("Mã giảng viên", "Họ tên", "Bộ môn"),
                lecturers.findExportRows(EXPORT_PAGE), r -> List.of(r.lecturerCode(), r.fullName(), nullable(r.department())));
    }

    public byte[] thesesExcel() {
        return workbook("DeTai", List.of("Đề tài", "Giảng viên hướng dẫn", "Năm học", "Học kỳ"),
                theses.findExportRows(EXPORT_PAGE), r -> List.of(r.title(), r.supervisorName(), r.academicYear(), r.semester()));
    }

    public byte[] councilsExcel() {
        return workbook("HoiDong", List.of("Hội đồng", "Năm học", "Học kỳ", "Số thành viên"),
                councils.findExportRows(EXPORT_PAGE), r -> List.of(r.name(), r.academicYear(), r.semester(), r.memberCount()));
    }

    public byte[] schedulesExcel(String academicYear, String semester) {
        List<ScheduleExportRow> rows = scheduleRows(academicYear, semester);
        return workbook("LichBaoVe", List.of("Đề tài", "Sinh viên", "Hội đồng", "Phòng", "Bắt đầu", "Kết thúc"),
                rows, r -> List.of(r.thesisTitle(), r.studentName(), r.councilName(), r.room(), r.startTime(), r.endTime()));
    }

    public byte[] resultsExcel(String academicYear, String semester) {
        List<ResultExportRow> rows = resultRows(academicYear, semester);
        return workbook("KetQua", List.of("Sinh viên", "Đề tài", "GVHD", "Hội đồng", "Điểm cuối", "Thời gian"),
                rows, r -> List.of(r.studentName(), r.thesisTitle(), r.supervisorName(), r.councilName(), r.finalScore(), r.defenseTime()));
    }

    public byte[] schedulesPdf(String academicYear, String semester) {
        List<String> lines = scheduleRows(academicYear, semester).stream()
                .map(r -> r.startTime().format(DATE_TIME) + " | " + r.room() + " | " + r.studentName()
                        + " | " + r.thesisTitle() + " | " + r.councilName()).toList();
        return pdf("LỊCH BẢO VỆ KHÓA LUẬN", filterLabel(academicYear, semester), lines);
    }

    public byte[] resultsPdf(String academicYear, String semester) {
        List<String> lines = resultRows(academicYear, semester).stream()
                .map(r -> r.studentName() + " | " + r.thesisTitle() + " | GVHD: " + r.supervisorName()
                        + " | " + r.councilName() + " | Điểm: " + r.finalScore()).toList();
        return pdf("KẾT QUẢ BẢO VỆ ĐÃ CÔNG BỐ", filterLabel(academicYear, semester), lines);
    }

    private List<ScheduleExportRow> scheduleRows(String year, String semester) {
        return schedules.findExportRows(filter(year), filter(semester), RegistrationStatus.APPROVED, EXPORT_PAGE);
    }

    private List<ResultExportRow> resultRows(String year, String semester) {
        return results.findPublishedExportRows(filter(year), filter(semester), RegistrationStatus.APPROVED, EXPORT_PAGE);
    }

    private <T> byte[] workbook(String sheetName, List<String> headers, List<T> data,
                                Function<T, List<Object>> mapper) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.setCompressTempFiles(true);
            Sheet sheet = workbook.createSheet(sheetName);
            sheet.createFreezePane(0, 1);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont(); headerFont.setBold(true); headerStyle.setFont(headerFont);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = header.createCell(i, CellType.STRING); cell.setCellValue(headers.get(i)); cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, Math.min(50, Math.max(14, headers.get(i).length() + 6)) * 256);
            }
            int rowNumber = 1;
            for (T item : data) {
                Row row = sheet.createRow(rowNumber++);
                List<Object> values = mapper.apply(item);
                for (int i = 0; i < values.size(); i++) writeCell(row.createCell(i), values.get(i), dateStyle);
            }
            workbook.write(output);
            workbook.dispose();
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo tệp Excel", exception);
        }
    }

    private void writeCell(Cell cell, Object value, CellStyle dateStyle) {
        if (value instanceof Number number) {
            cell.setCellType(CellType.NUMERIC); cell.setCellValue(number.doubleValue());
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(dateTime); cell.setCellStyle(dateStyle);
        } else {
            cell.setCellType(CellType.STRING); cell.setCellValue(safeSpreadsheetText(String.valueOf(value)));
        }
    }

    static String safeSpreadsheetText(String value) {
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) return "'" + value;
        return value;
    }

    private byte[] pdf(String title, String subtitle, List<String> sourceLines) {
        List<String> lines = new ArrayList<>();
        lines.add(title); lines.add(subtitle); lines.add("Ngày xuất: " + LocalDate.now()); lines.add("");
        lines.addAll(sourceLines.isEmpty() ? List.of("Không có dữ liệu phù hợp.") : sourceLines);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDType0Font font = loadUnicodeFont(document, "/fonts/DejaVuSans.ttf.b64");
            for (int offset = 0; offset < lines.size(); offset += 32) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText(); content.setFont(font, 9); content.setLeading(22);
                    content.newLineAtOffset(42, page.getMediaBox().getHeight() - 48);
                    for (String line : lines.subList(offset, Math.min(offset + 32, lines.size()))) {
                        content.showText(limitLine(line)); content.newLine();
                    }
                    content.endText();
                }
            }
            document.save(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể tạo tệp PDF", exception);
        }
    }

    static PDType0Font loadUnicodeFont(PDDocument document, String resourcePath) {
        try (InputStream encodedStream = ManagementExportService.class.getResourceAsStream(resourcePath)) {
            if (encodedStream == null) {
                throw new IllegalStateException("Không tìm thấy tài nguyên font PDF Unicode: " + resourcePath);
            }
            byte[] decoded;
            try {
                decoded = Base64.getMimeDecoder().decode(encodedStream.readAllBytes());
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Tài nguyên font PDF Unicode không phải Base64 hợp lệ", exception);
            }
            try (ByteArrayInputStream fontStream = new ByteArrayInputStream(decoded)) {
                return PDType0Font.load(document, fontStream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể đọc tài nguyên font PDF Unicode", exception);
        }
    }

    private String filter(String value) {
        if (value == null || value.isBlank()) return null;
        String clean = value.trim();
        if (clean.length() > 20 || !clean.matches("[\\p{L}\\p{N}._ -]+"))
            throw new DomainRuleViolationException("Bộ lọc năm học/học kỳ không hợp lệ");
        return clean;
    }

    private String filterLabel(String year, String semester) {
        String y = filter(year), s = filter(semester);
        return (y == null ? "Tất cả năm học" : "Năm học " + y) + " · " + (s == null ? "Tất cả học kỳ" : s);
    }

    private String limitLine(String value) {
        String safe = value.replaceAll("[\\p{Cc}\\p{Cf}]", " ").replaceAll("\\s+", " ").trim();
        return safe.length() <= 105 ? safe : safe.substring(0, 102) + "...";
    }
    private String nullable(String value) { return value == null ? "" : value; }
}
