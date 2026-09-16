package com.glucoze.thesismanagement.thesis.controller;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.thesis.dto.ReportForm;
import com.glucoze.thesismanagement.thesis.service.ThesisManagementService;
import com.glucoze.thesismanagement.thesis.service.ThesisQueryService;
import com.glucoze.thesismanagement.thesis.service.ReportFileStorage;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/student/reports")
public class StudentReportController {

    private final ThesisManagementService thesisManagementService;
    private final ThesisQueryService thesisQueryService;

    public StudentReportController(ThesisManagementService thesisManagementService,
                                   ThesisQueryService thesisQueryService) {
        this.thesisManagementService = thesisManagementService;
        this.thesisQueryService = thesisQueryService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("reports", thesisQueryService.listStudentReports(userDetails.getUsername()));
        model.addAttribute("registrations", thesisQueryService.listStudentRegistrations(userDetails.getUsername()));
        return "student/reports";
    }

    @GetMapping("/{registrationId}")
    public String form(@PathVariable Long registrationId,
                       @AuthenticationPrincipal UserDetails userDetails,
                       Model model) {
        model.addAttribute("reportForm", thesisQueryService.getReportForm(userDetails.getUsername(), registrationId));
        model.addAttribute("registrationId", registrationId);
        return "student/report-form";
    }

    @PostMapping("/{registrationId}")
    public String submit(@PathVariable Long registrationId,
                         @AuthenticationPrincipal UserDetails userDetails,
                         @Valid ReportForm reportForm,
                         BindingResult bindingResult,
                         @RequestParam(value = "file", required = false) MultipartFile file,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("registrationId", registrationId);
            return "student/report-form";
        }
        try {
            thesisManagementService.submitReport(userDetails.getUsername(), registrationId, reportForm, file);
        } catch (DomainRuleViolationException exception) {
            bindingResult.reject("report.invalid", exception.getMessage());
            model.addAttribute("registrationId", registrationId);
            return "student/report-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Nộp báo cáo thành công.");
        return "redirect:/student/reports";
    }

    @GetMapping("/file/{reportId}")
    public ResponseEntity<byte[]> file(@PathVariable Long reportId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        ReportFileStorage.StoredReportFile file =
                thesisManagementService.getStudentReportFile(userDetails.getUsername(), reportId);
        return downloadResponse(file);
    }

    private ResponseEntity<byte[]> downloadResponse(ReportFileStorage.StoredReportFile file) {
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.content());
    }
}
