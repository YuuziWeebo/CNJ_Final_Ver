package com.glucoze.thesismanagement.grading.controller;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.grading.dto.ScoreForm;
import com.glucoze.thesismanagement.grading.service.GradingService;
import com.glucoze.thesismanagement.grading.service.GradingQueryService;
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
@RequestMapping("/lecturer/grading")
public class LecturerGradingController {

    private final GradingService gradingService;
    private final GradingQueryService gradingQueryService;

    public LecturerGradingController(GradingService gradingService, GradingQueryService gradingQueryService) {
        this.gradingService = gradingService;
        this.gradingQueryService = gradingQueryService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("grades", gradingQueryService.listLecturerSchedules(userDetails.getUsername()));
        return "lecturer/grading";
    }

    @GetMapping("/{id}")
    public String form(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("grade", gradingQueryService.getLecturerGrade(userDetails.getUsername(), id));
        model.addAttribute("scoreForm", new ScoreForm());
        return "lecturer/grading-form";
    }

    @PostMapping("/{id}/supervisor-score")
    public String supervisorScore(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails,
                                  @Valid ScoreForm scoreForm, BindingResult result, Model model,
                                  RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("grade", gradingQueryService.getLecturerGrade(userDetails.getUsername(), id));
            return "lecturer/grading-form";
        }
        try {
            gradingService.enterSupervisorScore(userDetails.getUsername(), id, scoreForm);
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/lecturer/grading/{id}";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu điểm GVHD.");
        return "redirect:/lecturer/grading/{id}";
    }

    @PostMapping("/{id}/council-score")
    public String councilScore(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails,
                               @Valid ScoreForm scoreForm, BindingResult result, Model model,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("grade", gradingQueryService.getLecturerGrade(userDetails.getUsername(), id));
            return "lecturer/grading-form";
        }
        try {
            gradingService.enterCouncilScore(userDetails.getUsername(), id, scoreForm);
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/lecturer/grading/{id}";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu điểm hội đồng.");
        return "redirect:/lecturer/grading/{id}";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails,
                          RedirectAttributes redirectAttributes) {
        try {
            gradingService.publishResult(userDetails.getUsername(), id);
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/lecturer/grading/{id}";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã công bố kết quả cuối cùng.");
        return "redirect:/lecturer/grading/{id}";
    }
}
