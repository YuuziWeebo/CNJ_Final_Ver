package com.glucoze.thesismanagement.common.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class MvcExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new MvcExceptionHandler())
                .build();
    }

    @Test
    void rendersNotFoundErrorsConsistently() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/domain-error"))
                .andExpect(model().attribute("errorTitle", "Không tìm thấy dữ liệu"))
                .andExpect(model().attribute("errorMessage", "missing"));
    }

    @Test
    void rendersDomainRuleErrorsConsistently() throws Exception {
        mockMvc.perform(get("/test/domain"))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/domain-error"))
                .andExpect(model().attribute("errorMessage", "invalid"));
    }

    @Test
    void doesNotExposeDatabaseExceptionDetails() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(view().name("error/domain-error"))
                .andExpect(model().attributeDoesNotExist("exception"))
                .andExpect(model().attribute("errorMessage",
                        "Dữ liệu đã thay đổi hoặc đang được sử dụng. Vui lòng tải lại và thử lại."));
    }

    @Test
    void rendersFriendlyMessageWhenFrameworkRejectsOversizedUpload() throws Exception {
        mockMvc.perform(get("/test/upload-too-large"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(view().name("error/domain-error"))
                .andExpect(model().attribute("errorTitle", "Tệp tải lên quá lớn"))
                .andExpect(model().attribute("errorMessage",
                        "Tệp vượt quá giới hạn cho phép. Báo cáo tối đa 10 MB; ảnh đại diện tối đa 2 MB."));
    }

    @Controller
    static class ThrowingController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new ResourceNotFoundException("missing");
        }

        @GetMapping("/test/domain")
        String domain() {
            throw new DomainRuleViolationException("invalid");
        }

        @GetMapping("/test/conflict")
        String conflict() {
            throw new DataIntegrityViolationException("database detail");
        }

        @GetMapping("/test/upload-too-large")
        String uploadTooLarge() {
            throw new MaxUploadSizeExceededException(10L);
        }
    }
}
