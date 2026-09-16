package com.glucoze.thesismanagement.notification;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping("/notifications")
    public String list(@AuthenticationPrincipal UserDetails user, @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("notifications", service.listForUser(user.getUsername(), page));
        model.addAttribute("notificationRole", role(user));
        return "notifications/list";
    }

    @PostMapping("/notifications/{id}/read")
    public String markRead(@PathVariable Long id, @AuthenticationPrincipal UserDetails user) {
        service.markRead(user.getUsername(), id);
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead(@AuthenticationPrincipal UserDetails user) {
        service.markAllRead(user.getUsername());
        return "redirect:/notifications";
    }

    private String role(UserDetails user) {
        if (user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return "ADMIN";
        if (user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_LECTURER"))) return "LECTURER";
        return "STUDENT";
    }
}
