package com.glucoze.thesismanagement.council.controller;

import com.glucoze.thesismanagement.council.dto.CalendarEventView;
import com.glucoze.thesismanagement.council.service.DefenseCalendarService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DefenseCalendarController {
    private final DefenseCalendarService service;

    public DefenseCalendarController(DefenseCalendarService service) { this.service = service; }

    @GetMapping("/admin/schedules/calendar")
    public String adminPage(Model model) { return page(model, "ADMIN", "/admin/schedules/calendar/events"); }

    @GetMapping("/lecturer/schedules/calendar")
    public String lecturerPage(Model model) { return page(model, "LECTURER", "/lecturer/schedules/calendar/events"); }

    @GetMapping("/student/schedules/calendar")
    public String studentPage(Model model) { return page(model, "STUDENT", "/student/schedules/calendar/events"); }

    @GetMapping("/admin/schedules/calendar/events")
    @ResponseBody
    public List<CalendarEventView> adminEvents(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return service.adminEvents(start, end);
    }

    @GetMapping("/lecturer/schedules/calendar/events")
    @ResponseBody
    public List<CalendarEventView> lecturerEvents(@AuthenticationPrincipal UserDetails user,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return service.lecturerEvents(user.getUsername(), start, end);
    }

    @GetMapping("/student/schedules/calendar/events")
    @ResponseBody
    public List<CalendarEventView> studentEvents(@AuthenticationPrincipal UserDetails user,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return service.studentEvents(user.getUsername(), start, end);
    }

    private String page(Model model, String role, String eventsUrl) {
        model.addAttribute("calendarRole", role);
        model.addAttribute("calendarEventsUrl", eventsUrl);
        return "schedules/calendar";
    }
}
