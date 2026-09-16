package com.glucoze.thesismanagement.audit;

import com.glucoze.thesismanagement.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_created", columnList = "created_at"),
        @Index(name = "idx_audit_actor_created", columnList = "actor_username,created_at"),
        @Index(name = "idx_audit_action_created", columnList = "action,created_at"),
        @Index(name = "idx_audit_entity_created", columnList = "entity_type,created_at")
})
public class AuditLog extends BaseEntity {
    @Column(name = "actor_username", nullable = false, length = 100)
    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 40)
    private AuditEntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(nullable = false, length = 500)
    private String description;

    protected AuditLog() { }

    AuditLog(String actorUsername, AuditAction action, AuditEntityType entityType, Long entityId, String description) {
        this.actorUsername = actorUsername;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
    }

    public String getActorUsername() { return actorUsername; }
    public AuditAction getAction() { return action; }
    public AuditEntityType getEntityType() { return entityType; }
    public Long getEntityId() { return entityId; }
    public String getDescription() { return description; }
}
