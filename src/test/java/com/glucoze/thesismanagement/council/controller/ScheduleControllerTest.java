package com.glucoze.thesismanagement.council.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.glucoze.thesismanagement.council.dto.ScheduleForm;
import com.glucoze.thesismanagement.council.service.CouncilSchedulingService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ScheduleControllerTest {

    private CouncilSchedulingService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(CouncilSchedulingService.class);
        when(service.thesisOptions()).thenReturn(List.of());
        when(service.councilOptions()).thenReturn(List.of());
        mockMvc = MockMvcBuilders.standaloneSetup(new ScheduleController(service)).build();
    }

    @Test
    void invalidFormReturnsFieldErrorsWithoutCallingService() throws Exception {
        mockMvc.perform(post("/admin/schedules"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/schedule-form"))
                .andExpect(model().attributeHasFieldErrors(
                        "scheduleForm", "thesisId", "councilId", "room", "startTime", "endTime"));

        verify(service, never()).createSchedule(org.mockito.ArgumentMatchers.any(ScheduleForm.class));
    }
}
