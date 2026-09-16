package com.glucoze.thesismanagement.home.controller;

import com.glucoze.thesismanagement.home.service.DashboardQueryService;
import com.glucoze.thesismanagement.user.service.UserProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final DashboardQueryService dashboardQueryService;
    private final UserProfileService userProfileService;

    public HomeController(DashboardQueryService dashboardQueryService, UserProfileService userProfileService) {
        this.dashboardQueryService = dashboardQueryService;
        this.userProfileService = userProfileService;
    }

    @GetMapping({"/", "/home"})
    public String home(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("studentDashboard", false);
        model.addAttribute("adminDashboard", false);
        model.addAttribute("lecturerDashboard", false);
        if (userDetails == null) return "home";

        String username = userDetails.getUsername();
        if (hasRole(userDetails, "STUDENT")) {
            model.addAttribute("studentDashboard", true);
            model.addAttribute("studentProfile", userProfileService.getStudentProfile(username));
            model.addAttribute("dashboard", dashboardQueryService.studentDashboard(username));
        } else if (hasRole(userDetails, "LECTURER")) {
            model.addAttribute("lecturerDashboard", true);
            model.addAttribute("lecturerProfile", userProfileService.getLecturerProfile(username));
            model.addAttribute("dashboard", dashboardQueryService.lecturerDashboard(username));
        } else if (hasRole(userDetails, "ADMIN")) {
            model.addAttribute("adminDashboard", true);
            model.addAttribute("adminProfile", userProfileService.getAdminProfile(username));
            model.addAttribute("dashboard", dashboardQueryService.adminDashboard());
        }
        return "home";
    }

    private boolean hasRole(UserDetails userDetails, String role) {
        return userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
