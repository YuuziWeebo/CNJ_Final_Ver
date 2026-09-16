package com.glucoze.thesismanagement.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.config.JpaAuditingConfig;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import({JpaAuditingConfig.class, AuditLogService.class})
class AuditLogRepositoryTest {
    @Autowired AuditLogRepository repository;
    @Autowired AuditLogService service;
    @Autowired UserAccountRepository accounts;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach void clearSecurity() { SecurityContextHolder.clearContext(); }

    @Test void persistsAndFiltersSnapshotWithPaginationNewestFirst() {
        authenticate("admin01");
        service.append(AuditAction.USER_CREATE, AuditEntityType.USER_ACCOUNT, 1L, "Đã tạo tài khoản @student01");
        service.append(AuditAction.THESIS_CREATE, AuditEntityType.THESIS, 2L, "Đã tạo đề tài");
        repository.flush();

        assertThat(service.find("admin01", AuditAction.USER_CREATE, AuditEntityType.USER_ACCOUNT,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 0).getContent())
                .singleElement().satisfies(log -> {
                    assertThat(log.actorUsername()).isEqualTo("admin01");
                    assertThat(log.entityId()).isEqualTo(1L);
                });
        assertThat(service.find(null, null, null, null, null, 0).getSize()).isEqualTo(25);
    }

    @Test void actorSnapshotSurvivesAccountDeletionBecauseThereIsNoForeignKey() {
        UserAccount actor = accounts.saveAndFlush(new UserAccount("deleted-admin", "hash", Role.ADMIN));
        authenticate(actor.getUsername());
        service.append(AuditAction.USER_DELETE, AuditEntityType.USER_ACCOUNT, actor.getId(), "Đã xóa tài khoản @target");
        accounts.delete(actor);
        accounts.flush();
        assertThat(repository.findAll()).anySatisfy(log -> {
            assertThat(log.getActorUsername()).isEqualTo("deleted-admin");
            assertThat(log.getAction()).isEqualTo(AuditAction.USER_DELETE);
            assertThat(log.getEntityId()).isEqualTo(actor.getId());
        });
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void successCommitsAndTwoFailedTransactionsLeaveNoAuditRows() {
        authenticate("admin01");
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> service.append(AuditAction.COUNCIL_CREATE,
                AuditEntityType.COUNCIL, 1L, "Đã tạo hội đồng"));
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.append(AuditAction.SCHEDULE_CREATE, AuditEntityType.DEFENSE_SCHEDULE, 2L, "Đã tạo lịch bảo vệ");
            throw new IllegalStateException("schedule failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            service.append(AuditAction.RESULT_PUBLISH, AuditEntityType.RESULT, 3L, "Đã công bố kết quả");
            throw new IllegalStateException("publish failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(repository.findAll()).singleElement()
                .extracting(AuditLog::getAction).isEqualTo(AuditAction.COUNCIL_CREATE);
    }

    @Test void rejectsInvertedDateRange() {
        assertThatThrownBy(() -> service.find(null, null, null, LocalDate.now(), LocalDate.now().minusDays(1), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(username, "n/a", java.util.List.of()));
    }
}
