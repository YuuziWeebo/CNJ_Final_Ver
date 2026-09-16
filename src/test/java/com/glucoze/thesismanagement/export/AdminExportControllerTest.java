package com.glucoze.thesismanagement.export;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminExportController.class)
@Import(com.glucoze.thesismanagement.config.SecurityConfig.class)
class AdminExportControllerTest {
    @Autowired MockMvc mockMvc; @MockBean ManagementExportService service; @MockBean NotificationService notifications;
    @MockBean UserAccountRepository accounts; @MockBean StudentRepository students; @MockBean LecturerRepository lecturers; @MockBean AdminRepository admins;

    @Test void onlyAdminCanAccessExports() throws Exception {
        when(service.studentsExcel()).thenReturn(minimalWorkbook());
        mockMvc.perform(get("/admin/exports/students.xlsx")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/admin/exports/students.xlsx").with(user("sv").roles("STUDENT"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/exports/students.xlsx").with(user("gv").roles("LECTURER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/exports/students.xlsx").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test void responseUsesSafeAttachmentHeadersAndMimeTypes() throws Exception {
        org.mockito.Mockito.when(service.schedulesExcel(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(new byte[]{1, 2});
        mockMvc.perform(get("/admin/exports/schedules.xlsx?academicYear=2026-2027&semester=HK1")
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(header().string("Content-Type", AdminExportController.XLSX.toString()))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("X-Evil"))));
        org.assertj.core.api.Assertions.assertThat(AdminExportController.filteredFilename(
                "lich-bao-ve", "2026\r\nX-Evil: yes", "HK1", "xlsx"))
                .matches("[a-z0-9.-]+").doesNotContain("\r", "\n", ":");
    }

    @Test void pdfResponseUsesPdfMimeAndServerFilename() throws Exception {
        org.mockito.Mockito.when(service.resultsPdf(null, null)).thenReturn("%PDF".getBytes());
        mockMvc.perform(get("/admin/exports/results.pdf").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("ket-qua-bao-ve.pdf")));
    }

    private byte[] minimalWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            workbook.createSheet("SinhVien");
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
