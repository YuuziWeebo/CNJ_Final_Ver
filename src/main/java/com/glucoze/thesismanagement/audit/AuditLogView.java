package com.glucoze.thesismanagement.audit;

import java.time.LocalDateTime;

public record AuditLogView(Long id, String actorUsername, AuditAction action, AuditEntityType entityType,
                           Long entityId, String description, LocalDateTime createdAt) {
}
