package com.glucoze.thesismanagement.user.controller;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.user.dto.StudentProfileForm;
import com.glucoze.thesismanagement.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/student/profile")
public class StudentProfileController {

    private final UserProfileService userProfileService;

    public StudentProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public String form(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("profileForm", userProfileService.getStudentProfile(userDetails.getUsername()));
        return "student/profile";
    }

    @PostMapping
    public String update(@AuthenticationPrincipal UserDetails userDetails,
                         @Valid @ModelAttribute("profileForm") StudentProfileForm profileForm,
                         BindingResult bindingResult,
                         @RequestParam(value = "avatar", required = false) MultipartFile avatar,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "student/profile";
        }
        try {
            userProfileService.updateStudentProfileAndAvatar(userDetails.getUsername(), profileForm, avatar);
        } catch (DomainRuleViolationException exception) {
            bindingResult.reject("profile.invalid", exception.getMessage());
            return "student/profile";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công.");
        return "redirect:/student/profile";
    }
}
