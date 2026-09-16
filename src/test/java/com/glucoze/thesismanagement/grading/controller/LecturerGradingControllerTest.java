package com.glucoze.thesismanagement.grading.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.glucoze.thesismanagement.grading.service.GradingQueryService;
import com.glucoze.thesismanagement.grading.service.GradingService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.ConcurrentModel;

class LecturerGradingControllerTest {

    @Test
    void gradingDetailPassesAuthenticatedLecturerToScopedQuery() {
        GradingService gradingService = mock(GradingService.class);
        GradingQueryService queryService = mock(GradingQueryService.class);
        UserDetails lecturer = mock(UserDetails.class);
        org.mockito.Mockito.when(lecturer.getUsername()).thenReturn("lecturer01");
        LecturerGradingController controller = new LecturerGradingController(gradingService, queryService);

        controller.form(42L, lecturer, new ConcurrentModel());

        verify(queryService).getLecturerGrade("lecturer01", 42L);
    }
}
