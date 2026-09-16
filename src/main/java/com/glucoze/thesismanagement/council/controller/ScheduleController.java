package com.glucoze.thesismanagement.council.controller;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.council.dto.ScheduleForm;
import com.glucoze.thesismanagement.council.service.CouncilSchedulingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/schedules")
public class ScheduleController {

    private final CouncilSchedulingService service;

    public ScheduleController(CouncilSchedulingService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("schedules", service.listSchedules());
        return "admin/schedules";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("scheduleForm", new ScheduleForm());
        model.addAttribute("theses", service.thesisOptions());
        model.addAttribute("councils", service.councilOptions());
        return "admin/schedule-form";
    }

    @PostMapping
    public String create(@Valid ScheduleForm form, BindingResult bindingResult,
                         Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateFormOptions(model);
            return "admin/schedule-form";
        }
        try {
            service.createSchedule(form);
        } catch (DomainRuleViolationException exception) {
            populateFormOptions(model);
            model.addAttribute("errorMessage", exception.getMessage());
            return "admin/schedule-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Tạo lịch bảo vệ thành công.");
        return "redirect:/admin/schedules";
    }

    private void populateFormOptions(Model model) {
        model.addAttribute("theses", service.thesisOptions());
        model.addAttribute("councils", service.councilOptions());
    }
}
