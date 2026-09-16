package com.glucoze.thesismanagement.user.controller;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import com.glucoze.thesismanagement.user.dto.AdminUserForm;
import com.glucoze.thesismanagement.user.dto.AdminUserEditForm;
import com.glucoze.thesismanagement.user.service.UserManagementService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserManagementService userManagementService;

    public AdminUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("users", userManagementService.listAccounts());
        return "admin/users";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("userForm", new AdminUserForm());
        return "admin/user-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("userForm", userManagementService.getAccountForEdit(id));
        return "admin/user-edit-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("userForm") AdminUserForm userForm,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "admin/user-form";
        }
        try {
            userManagementService.createAccount(userForm);
        } catch (DomainRuleViolationException exception) {
            bindingResult.reject("account.invalid", exception.getMessage());
            return "admin/user-form";
        } catch (DataIntegrityViolationException exception) {
            bindingResult.reject("account.invalid", "Thông tin tài khoản đã tồn tại hoặc không hợp lệ.");
            return "admin/user-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản thành công.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("userForm") AdminUserEditForm userForm,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        userForm.setId(id);
        if (bindingResult.hasErrors()) {
            return "admin/user-edit-form";
        }
        try {
            userManagementService.updateAccount(userForm, userDetails.getUsername());
        } catch (DomainRuleViolationException exception) {
            bindingResult.reject("account.invalid", exception.getMessage());
            return "admin/user-edit-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật tài khoản thành công.");
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            userManagementService.deleteAccount(id, userDetails.getUsername());
        } catch (DomainRuleViolationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/admin/users";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Xóa tài khoản thành công.");
        return "redirect:/admin/users";
    }
}
