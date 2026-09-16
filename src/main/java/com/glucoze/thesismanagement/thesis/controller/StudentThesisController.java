package com.glucoze.thesismanagement.thesis.controller;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.thesis.service.ThesisManagementService;
import com.glucoze.thesismanagement.thesis.service.ThesisQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/theses")
public class StudentThesisController {

    private final ThesisManagementService thesisManagementService;
    private final ThesisQueryService thesisQueryService;

    public StudentThesisController(ThesisManagementService thesisManagementService,
                               ThesisQueryService thesisQueryService) {
        this.thesisManagementService = thesisManagementService;
        this.thesisQueryService = thesisQueryService;
    }

    @GetMapping
    public String browse(@RequestParam(name = "namHoc", required = false) String academicYear,
                         @RequestParam(name = "hocKy", required = false) String semester,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        model.addAttribute("theses", thesisQueryService.browseTheses(academicYear, semester));
        model.addAttribute("registrations", thesisQueryService.listStudentRegistrations(userDetails.getUsername()));
        model.addAttribute("activeThesisId", thesisQueryService.activeStudentThesisId(userDetails.getUsername()));
        model.addAttribute("academicYear", academicYear);
        model.addAttribute("semester", semester);
        return "student/theses";
    }

    @PostMapping("/{id}/register")
    public String register(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        try {
            thesisManagementService.registerForThesis(userDetails.getUsername(), id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi đăng ký đề tài.");
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/student/theses";
    }
}
