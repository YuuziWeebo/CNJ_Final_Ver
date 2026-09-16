package com.glucoze.thesismanagement.grading.controller;

import com.glucoze.thesismanagement.grading.service.GradingQueryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/student/grading")
public class StudentGradingController {

    private final GradingQueryService gradingQueryService;

    public StudentGradingController(GradingQueryService gradingQueryService) {
        this.gradingQueryService = gradingQueryService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("grades", gradingQueryService.listStudentGrades(userDetails.getUsername()));
        return "student/grading";
    }
}
