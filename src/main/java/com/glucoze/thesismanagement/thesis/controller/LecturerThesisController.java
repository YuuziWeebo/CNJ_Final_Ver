package com.glucoze.thesismanagement.thesis.controller;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.thesis.dto.ThesisForm;
import com.glucoze.thesismanagement.thesis.service.ThesisManagementService;
import com.glucoze.thesismanagement.thesis.service.ThesisQueryService;
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

@Controller
@RequestMapping("/lecturer/theses")
public class LecturerThesisController {

    private final ThesisManagementService thesisManagementService;
    private final ThesisQueryService thesisQueryService;

    public LecturerThesisController(ThesisManagementService thesisManagementService,
                               ThesisQueryService thesisQueryService) {
        this.thesisManagementService = thesisManagementService;
        this.thesisQueryService = thesisQueryService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("theses", thesisQueryService.listLecturerTheses(userDetails.getUsername()));
        return "lecturer/theses";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("thesisForm", new ThesisForm());
        return "lecturer/thesis-form";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid ThesisForm thesisForm,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "lecturer/thesis-form";
        }
        thesisManagementService.createThesis(userDetails.getUsername(), thesisForm);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo đề tài thành công.");
        return "redirect:/lecturer/theses";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal UserDetails userDetails,
                           @PathVariable Long id,
                           Model model) {
        model.addAttribute("thesisForm", thesisQueryService.getThesisForm(userDetails.getUsername(), id));
        model.addAttribute("thesisId", id);
        return "lecturer/thesis-form";
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Long id,
                         @Valid ThesisForm thesisForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("thesisId", id);
            return "lecturer/thesis-form";
        }
        thesisManagementService.updateThesis(userDetails.getUsername(), id, thesisForm);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật đề tài thành công.");
        return "redirect:/lecturer/theses";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal UserDetails userDetails,
                         @PathVariable Long id,
                         RedirectAttributes redirectAttributes) {
        try {
            thesisManagementService.deleteThesis(userDetails.getUsername(), id);
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/lecturer/theses";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Xóa đề tài thành công.");
        return "redirect:/lecturer/theses";
    }
}
