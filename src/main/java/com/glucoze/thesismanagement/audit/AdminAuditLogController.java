package com.glucoze.thesismanagement.audit;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/audit-logs")
public class AdminAuditLogController {
    private final AuditLogService service;

    public AdminAuditLogController(AuditLogService service) { this.service = service; }

    @GetMapping
    public String list(@RequestParam(required = false) String actor,
                       @RequestParam(required = false) AuditAction action,
                       @RequestParam(required = false) AuditEntityType entityType,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("auditLogs", service.find(actor, action, entityType, from, to, page));
        model.addAttribute("actions", AuditAction.values());
        model.addAttribute("entityTypes", AuditEntityType.values());
        model.addAttribute("actor", actor);
        model.addAttribute("selectedAction", action);
        model.addAttribute("selectedEntityType", entityType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admin/audit-logs";
    }
}
