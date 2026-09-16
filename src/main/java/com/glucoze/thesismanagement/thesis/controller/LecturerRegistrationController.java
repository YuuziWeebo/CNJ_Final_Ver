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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/lecturer/registrations")
public class LecturerRegistrationController {

    private final ThesisManagementService thesisManagementService;
    private final ThesisQueryService thesisQueryService;

    public LecturerRegistrationController(ThesisManagementService thesisManagementService,
                               ThesisQueryService thesisQueryService) {
        this.thesisManagementService = thesisManagementService;
        this.thesisQueryService = thesisQueryService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("registrations", thesisQueryService.listLecturerRegistrations(userDetails.getUsername()));
        return "lecturer/registrations";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        try {
            thesisManagementService.approveRegistration(userDetails.getUsername(), id);
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/lecturer/registrations";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã duyệt đăng ký.");
        return "redirect:/lecturer/registrations";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            thesisManagementService.rejectRegistration(userDetails.getUsername(), id);
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/lecturer/registrations";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đăng ký.");
        return "redirect:/lecturer/registrations";
    }
}
