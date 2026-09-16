package com.glucoze.thesismanagement.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.thesis.controller.StudentReportController;
import com.glucoze.thesismanagement.thesis.service.ThesisManagementService;
import com.glucoze.thesismanagement.thesis.service.ThesisQueryService;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import com.glucoze.thesismanagement.user.service.UserManagementService;
import com.glucoze.thesismanagement.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.glucoze.thesismanagement.notification.NotificationService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
        AdminUserController.class,
        AdminProfileController.class,
        LecturerProfileController.class,
        StudentProfileController.class,
        StudentReportController.class
})
class MvcFormRenderingRegressionTest {
    @MockBean NotificationService notificationService;

    @Autowired MockMvc mockMvc;

    @MockBean UserManagementService userManagementService;
    @MockBean UserProfileService userProfileService;
    @MockBean ThesisManagementService thesisManagementService;
    @MockBean ThesisQueryService thesisQueryService;
    @MockBean UserAccountRepository accountRepository;
    @MockBean AdminRepository adminRepository;
    @MockBean LecturerRepository lecturerRepository;
    @MockBean StudentRepository studentRepository;

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminEditDomainErrorRendersBoundFormWithPathId() throws Exception {
        doThrow(new DomainRuleViolationException("Tên không hợp lệ"))
                .when(userManagementService).updateAccount(any(), eq("admin"));

        mockMvc.perform(post("/admin/users/42")
                        .with(csrf())
                        .param("fullName", "Quản trị viên")
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-edit-form"))
                .andExpect(model().attributeExists("userForm"))
                .andExpect(model().attributeHasErrors("userForm"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/admin/users/42\"")));
    }

    @Test
    @WithMockUser(username = "student01", roles = "STUDENT")
    void invalidReportPostRendersBoundFormWithRegistrationId() throws Exception {
        mockMvc.perform(post("/student/reports/9")
                        .with(csrf())
                        .param("reportFile", "http://khong-an-toan.example"))
                .andExpect(status().isOk())
                .andExpect(view().name("student/report-form"))
                .andExpect(model().attributeExists("reportForm", "registrationId"))
                .andExpect(model().attribute("registrationId", 9L))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/student/reports/9\"")));
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void invalidAdminProfilePostRendersProfileForm() throws Exception {
        mockMvc.perform(post("/admin/profile").with(csrf()).param("fullName", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/profile"))
                .andExpect(model().attributeExists("profileForm"))
                .andExpect(model().attributeHasFieldErrors("profileForm", "fullName"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<form")));
    }

    @Test
    @WithMockUser(username = "lecturer01", roles = "LECTURER")
    void invalidLecturerProfilePostRendersProfileForm() throws Exception {
        assertInvalidProfileRenders("/lecturer/profile", "lecturer/profile", "lecturerCode", "", "   ");
    }

    @Test
    @WithMockUser(username = "student01", roles = "STUDENT")
    void invalidStudentProfilePostRendersProfileForm() throws Exception {
        assertInvalidProfileRenders("/student/profile", "student/profile", "studentCode", "", "");
    }

    private void assertInvalidProfileRenders(String path, String expectedView,
                                             String invalidField, String invalidValue,
                                             String fullName) throws Exception {
        mockMvc.perform(post(path)
                        .with(csrf())
                        .param("fullName", fullName)
                        .param(invalidField, invalidValue))
                .andExpect(status().isOk())
                .andExpect(view().name(expectedView))
                .andExpect(model().attributeExists("profileForm"))
                .andExpect(model().attributeHasFieldErrors("profileForm", invalidField))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<form")));
    }
}
