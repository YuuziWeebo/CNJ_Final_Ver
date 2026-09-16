package com.glucoze.thesismanagement.council.controller;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.council.dto.CouncilForm;
import com.glucoze.thesismanagement.council.dto.MemberForm;
import com.glucoze.thesismanagement.council.service.CouncilSchedulingService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.validation.annotation.Validated;

@Controller
@RequestMapping("/admin/councils")
public class CouncilController {

    private final CouncilSchedulingService service;

    public CouncilController(CouncilSchedulingService service) {
        this.service = service;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("councils", service.listCouncils());
        return "admin/councils";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("councilForm", new CouncilForm());
        model.addAttribute("lecturers", service.lecturerOptions());
        return "admin/council-form";
    }

    @PostMapping
    public String create(@Validated(CouncilForm.Create.class) CouncilForm form, BindingResult result, Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("lecturers", service.lecturerOptions());
            return "admin/council-form";
        }
        try {
            service.createCouncil(form);
        } catch (DomainRuleViolationException exception) {
            result.reject("council.invalid", exception.getMessage());
            model.addAttribute("lecturers", service.lecturerOptions());
            return "admin/council-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Tạo hội đồng thành công.");
        return "redirect:/admin/councils";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("councilForm", service.getCouncilForm(id));
        model.addAttribute("councilId", id);
        model.addAttribute("lecturers", service.lecturerOptions());
        return "admin/council-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid CouncilForm form, BindingResult result,
                         Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("councilId", id);
            model.addAttribute("lecturers", service.lecturerOptions());
            return "admin/council-form";
        }
        service.updateCouncil(id, form);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hội đồng thành công.");
        return "redirect:/admin/councils";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.deleteCouncil(id);
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/admin/councils";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Xóa hội đồng thành công.");
        return "redirect:/admin/councils";
    }

    @GetMapping("/{id}/members")
    public String members(@PathVariable Long id, Model model) {
        model.addAttribute("members", service.listMembers(id));
        model.addAttribute("memberForm", new MemberForm());
        model.addAttribute("councilId", id);
        model.addAttribute("lecturers", service.lecturerOptions());
        return "admin/council-members";
    }

    @PostMapping("/{id}/members")
    public String addMember(@PathVariable Long id, @Valid MemberForm form, BindingResult result,
                            Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng chọn giảng viên và vai trò.");
            return "redirect:/admin/councils/{id}/members";
        }
        try {
            service.addMember(id, form);
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/admin/councils/{id}/members";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã thêm thành viên hội đồng.");
        return "redirect:/admin/councils/{id}/members";
    }

    @PostMapping("/{id}/members/{memberId}/delete")
    public String deleteMember(@PathVariable Long id, @PathVariable Long memberId,
                               RedirectAttributes redirectAttributes) {
        try {
            service.deleteMember(id, memberId);
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/admin/councils/{id}/members";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã gỡ thành viên hội đồng.");
        return "redirect:/admin/councils/{id}/members";
    }
}
