package com.glucoze.thesismanagement.thesis.controller;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.thesis.dto.ReviewForm;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/lecturer/reports")
public class LecturerReportController {

    private final ThesisManagementService thesisManagementService;
    private final ThesisQueryService thesisQueryService;

    public LecturerReportController(ThesisManagementService thesisManagementService,
                               ThesisQueryService thesisQueryService) {
        this.thesisManagementService = thesisManagementService;
        this.thesisQueryService = thesisQueryService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("reports", thesisQueryService.listLecturerReports(userDetails.getUsername()));
        model.addAttribute("reviewForm", new ReviewForm());
        return "lecturer/reports";
    }

    @PostMapping("/{id}/request-changes")
    public String requestChanges(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 @Valid ReviewForm reviewForm,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nội dung nhận xét không hợp lệ.");
            return "redirect:/lecturer/reports";
        }
        try {
            thesisManagementService.requestReportChanges(userDetails.getUsername(), id, reviewForm);
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/lecturer/reports";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã yêu cầu chỉnh sửa báo cáo.");
        return "redirect:/lecturer/reports";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetails userDetails,
                          @Valid ReviewForm reviewForm,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Nội dung nhận xét không hợp lệ.");
            return "redirect:/lecturer/reports";
        }
        try {
            thesisManagementService.approveReport(userDetails.getUsername(), id, reviewForm);
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/lecturer/reports";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt báo cáo.");
        return "redirect:/lecturer/reports";
    }

    @GetMapping("/file/{reportId}")
    public ResponseEntity<byte[]> file(@PathVariable Long reportId,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        ReportFileStorage.StoredReportFile file =
                thesisManagementService.getLecturerReportFile(userDetails.getUsername(), reportId);
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
