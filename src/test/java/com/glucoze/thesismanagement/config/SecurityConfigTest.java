package com.glucoze.thesismanagement.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.glucoze.thesismanagement.council.service.CouncilSchedulingService;
import com.glucoze.thesismanagement.grading.service.GradingQueryService;
import com.glucoze.thesismanagement.home.controller.HomeController;
import com.glucoze.thesismanagement.home.service.DashboardQueryService;
import com.glucoze.thesismanagement.thesis.service.ThesisQueryService;
import com.glucoze.thesismanagement.user.service.UserManagementService;
import com.glucoze.thesismanagement.user.service.UserProfileService;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.glucoze.thesismanagement.notification.NotificationService;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {
    @MockBean NotificationService notificationService;
    @MockBean DashboardQueryService dashboardQueryService;

    @MockBean
    private ThesisQueryService thesisQueryService;

    @MockBean
    private GradingQueryService gradingQueryService;

    @MockBean
    private UserManagementService userManagementService;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private CouncilSchedulingService councilSchedulingService;

    @MockBean private UserAccountRepository userAccountRepository;
    @MockBean private AdminRepository adminRepository;
    @MockBean private LecturerRepository lecturerRepository;
    @MockBean private StudentRepository studentRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void allowsPublicHome() throws Exception {
        mockMvc.perform(get("/home"))
                .andExpect(status().isOk());
    }

    @Test
    void redirectsAnonymousUsersFromProtectedAreaToLogin() throws Exception {
        mockMvc.perform(get("/student/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void doesNotExposeUploadsAsPublicResources() throws Exception {
        mockMvc.perform(get("/uploads/reports/private.pdf"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void deniesLecturerAccessToStudentArea() throws Exception {
        mockMvc.perform(get("/student/profile").with(user("lecturer01").roles("LECTURER")))
                .andExpect(status().isForbidden());
    }
}
