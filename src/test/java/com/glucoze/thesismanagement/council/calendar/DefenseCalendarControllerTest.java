package com.glucoze.thesismanagement.council.calendar;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.glucoze.thesismanagement.council.controller.DefenseCalendarController;
import com.glucoze.thesismanagement.council.service.DefenseCalendarService;
import com.glucoze.thesismanagement.notification.NotificationService;
import com.glucoze.thesismanagement.user.repository.AdminRepository;
import com.glucoze.thesismanagement.user.repository.LecturerRepository;
import com.glucoze.thesismanagement.user.repository.StudentRepository;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import static org.mockito.Mockito.when;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DefenseCalendarController.class)
@Import(com.glucoze.thesismanagement.config.SecurityConfig.class)
class DefenseCalendarControllerTest {
    @Autowired MockMvc mockMvc; @MockBean DefenseCalendarService service; @MockBean NotificationService notifications;
    @MockBean UserAccountRepository accounts; @MockBean StudentRepository students; @MockBean LecturerRepository lecturers; @MockBean AdminRepository admins;
    private static final String RANGE = "?start=2026-09-01T00:00:00&end=2026-10-01T00:00:00";

    @Test void anonymousCannotAccessPagesOrJson() throws Exception {
        mockMvc.perform(get("/admin/schedules/calendar")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/student/schedules/calendar/events" + RANGE)).andExpect(status().is3xxRedirection());
    }

    @Test void eachRoleCanOnlyAccessItsOwnPageAndDataEndpoint() throws Exception {
        mockMvc.perform(get("/admin/schedules/calendar").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mockMvc.perform(get("/lecturer/schedules/calendar").with(user("gv").roles("LECTURER"))).andExpect(status().isOk());
        mockMvc.perform(get("/student/schedules/calendar").with(user("sv").roles("STUDENT"))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/schedules/calendar").with(user("gv").roles("LECTURER"))).andExpect(status().isForbidden());
        mockMvc.perform(get("/lecturer/schedules/calendar/events" + RANGE).with(user("sv").roles("STUDENT"))).andExpect(status().isForbidden());
    }

    @Test void jsonDelegatesWithPrincipalRatherThanClientOwnerId() throws Exception {
        when(service.lecturerEvents(org.mockito.ArgumentMatchers.eq("gv"), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        mockMvc.perform(get("/lecturer/schedules/calendar/events" + RANGE + "&lecturerId=999")
                .with(user("gv").roles("LECTURER"))).andExpect(status().isOk());
        org.mockito.Mockito.verify(service).lecturerEvents(org.mockito.ArgumentMatchers.eq("gv"),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.any(LocalDateTime.class));
    }

    @Test void malformedDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/schedules/calendar/events?start=bad&end=2026-10-01T00:00:00")
                .with(user("admin").roles("ADMIN"))).andExpect(status().isBadRequest());
    }
}
