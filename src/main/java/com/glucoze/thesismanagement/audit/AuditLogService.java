package com.glucoze.thesismanagement.audit;

import com.glucoze.thesismanagement.common.exception.DomainRuleViolationException;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {
    private static final int PAGE_SIZE = 25;
    private final AuditLogRepository repository;

    public AuditLogService(AuditLogRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(AuditAction action, AuditEntityType entityType, Long entityId, String safeDescription) {
        repository.save(new AuditLog(currentActor(), action, entityType, entityId, safeDescription));
    }

    @Transactional(readOnly = true)
    public Page<AuditLogView> find(String actor, AuditAction action, AuditEntityType entityType,
                                   LocalDate from, LocalDate to, int requestedPage) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new DomainRuleViolationException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }
        String normalizedActor = actor == null || actor.isBlank() ? null : actor.trim();
        return repository.findAll((root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (normalizedActor != null) predicates.add(builder.equal(root.get("actorUsername"), normalizedActor));
            if (action != null) predicates.add(builder.equal(root.get("action"), action));
            if (entityType != null) predicates.add(builder.equal(root.get("entityType"), entityType));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay()));
            if (to != null) predicates.add(builder.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay()));
            return builder.and(predicates.toArray(Predicate[]::new));
        }, PageRequest.of(Math.max(0, requestedPage), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toView);
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "system";
        }
        return authentication.getName();
    }

    private AuditLogView toView(AuditLog log) {
        return new AuditLogView(log.getId(), log.getActorUsername(), log.getAction(), log.getEntityType(),
                log.getEntityId(), log.getDescription(), log.getCreatedAt());
    }
}
