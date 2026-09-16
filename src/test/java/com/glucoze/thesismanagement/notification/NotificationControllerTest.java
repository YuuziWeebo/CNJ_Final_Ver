package com.glucoze.thesismanagement.notification;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@WebMvcTest(NotificationController.class)
@Import(com.glucoze.thesismanagement.config.SecurityConfig.class)
class NotificationControllerTest {
    @Autowired MockMvc mockMvc;
    @MockBean NotificationService service;
    @MockBean UserAccountRepository accounts; @MockBean StudentRepository students;
    @MockBean LecturerRepository lecturers; @MockBean AdminRepository admins;

    @Test void anonymousCannotListNotifications() throws Exception {
        mockMvc.perform(get("/notifications")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test void authenticatedUserCanListNotifications() throws Exception {
        org.mockito.Mockito.when(service.listForUser("owner", 0)).thenReturn(org.springframework.data.domain.Page.empty());
        mockMvc.perform(get("/notifications").with(user("owner").roles("STUDENT"))).andExpect(status().isOk());
    }

    @Test void markReadRequiresCsrf() throws Exception {
        mockMvc.perform(post("/notifications/1/read").with(user("owner").roles("STUDENT")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/notifications/1/read").with(user("owner").roles("STUDENT")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        org.mockito.Mockito.verify(service).markRead("owner", 1L);
    }
}
