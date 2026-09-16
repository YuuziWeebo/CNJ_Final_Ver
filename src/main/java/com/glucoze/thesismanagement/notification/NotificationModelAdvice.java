package com.glucoze.thesismanagement.notification;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NotificationModelAdvice {
    private final NotificationService service;

    public NotificationModelAdvice(NotificationService service) { this.service = service; }

    @ModelAttribute
    public void notificationMetadata(@AuthenticationPrincipal UserDetails user, Model model) {
        model.addAttribute("notificationUnreadCount", user == null ? 0L : service.unreadCount(user.getUsername()));
    }
}
