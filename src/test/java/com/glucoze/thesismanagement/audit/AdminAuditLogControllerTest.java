package com.glucoze.thesismanagement.audit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminAuditLogController.class)
@Import(com.glucoze.thesismanagement.config.SecurityConfig.class)
class AdminAuditLogControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean AuditLogService service; @MockBean NotificationService notifications;
    @MockBean UserAccountRepository accounts; @MockBean StudentRepository students;
    @MockBean LecturerRepository lecturers; @MockBean AdminRepository admins;

    @Test void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin/audit-logs")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test void studentAndLecturerAreForbidden() throws Exception {
        mockMvc.perform(get("/admin/audit-logs").with(user("sv").roles("STUDENT"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/audit-logs").with(user("gv").roles("LECTURER"))).andExpect(status().isForbidden());
    }

    @Test void adminCanUseFiltersButMalformedDateIsRejected() throws Exception {
        org.mockito.Mockito.when(service.find(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt())).thenReturn(org.springframework.data.domain.Page.empty());
        mockMvc.perform(get("/admin/audit-logs?action=USER_CREATE&entityType=USER_ACCOUNT&from=2026-01-01")
                .with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/audit-logs?from=bad-date").with(user("admin").roles("ADMIN")))
                .andExpect(status().isBadRequest());
    }
}
