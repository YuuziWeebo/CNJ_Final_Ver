package com.glucoze.thesismanagement.home.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.glucoze.thesismanagement.home.dto.AdminDashboardView;
import com.glucoze.thesismanagement.home.dto.LecturerDashboardView;
import com.glucoze.thesismanagement.home.dto.StudentDashboardView;
import com.glucoze.thesismanagement.home.service.DashboardQueryService;
import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.user.dto.AdminProfileForm;
import com.glucoze.thesismanagement.user.dto.LecturerProfileForm;
import com.glucoze.thesismanagement.user.dto.StudentProfileForm;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import com.glucoze.thesismanagement.user.service.UserProfileService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@Import(com.glucoze.thesismanagement.config.SecurityConfig.class)
class HomeControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean DashboardQueryService dashboardQueryService;
    @MockBean UserProfileService userProfileService;
    @MockBean NotificationService notificationService;
    @MockBean UserAccountRepository userAccountRepository;
    @MockBean StudentRepository studentRepository;
    @MockBean LecturerRepository lecturerRepository;
    @MockBean AdminRepository adminRepository;

    @Test
    void anonymousHomeDoesNotQueryDashboardData() throws Exception {
        mockMvc.perform(get("/home")).andExpect(status().isOk())
                .andExpect(model().attribute("studentDashboard", false))
                .andExpect(model().attribute("lecturerDashboard", false))
                .andExpect(model().attribute("adminDashboard", false));
        verifyNoInteractions(dashboardQueryService, userProfileService);
    }

    @Test
    void lecturerOnlyReceivesLecturerScopedDashboard() throws Exception {
        var dashboard = new LecturerDashboardView(2, 1, 3, 4, List.of());
        when(userProfileService.getLecturerProfile("gv")).thenReturn(new LecturerProfileForm("GV", "GV01", null, null));
        when(dashboardQueryService.lecturerDashboard("gv")).thenReturn(dashboard);
        mockMvc.perform(get("/home").with(user("gv").roles("LECTURER"))).andExpect(status().isOk())
                .andExpect(model().attribute("lecturerDashboard", true)).andExpect(model().attribute("dashboard", dashboard));
        verify(dashboardQueryService).lecturerDashboard("gv");
    }

    @Test
    void studentOnlyReceivesStudentScopedDashboard() throws Exception {
        var dashboard = new StudentDashboardView(null, null, null, null, null, null, false, null);
        when(userProfileService.getStudentProfile("sv")).thenReturn(new StudentProfileForm("SV", "SV01", null, null));
        when(dashboardQueryService.studentDashboard("sv")).thenReturn(dashboard);
        mockMvc.perform(get("/home").with(user("sv").roles("STUDENT"))).andExpect(status().isOk())
                .andExpect(model().attribute("studentDashboard", true)).andExpect(model().attribute("dashboard", dashboard));
    }

    @Test
    void adminUsesAggregateDashboardView() throws Exception {
        var dashboard = new AdminDashboardView(10, 5, 8, 2, 3, 2, 4, 1, List.of());
        when(userProfileService.getAdminProfile("admin")).thenReturn(new AdminProfileForm("Admin", null));
        when(dashboardQueryService.adminDashboard()).thenReturn(dashboard);
        mockMvc.perform(get("/home").with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(model().attribute("adminDashboard", true)).andExpect(model().attribute("dashboard", dashboard));
    }
}
