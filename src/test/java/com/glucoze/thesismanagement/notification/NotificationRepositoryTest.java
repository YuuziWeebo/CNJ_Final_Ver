package com.glucoze.thesismanagement.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.config.JpaAuditingConfig;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import com.glucoze.thesismanagement.user.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class NotificationRepositoryTest {
    @Autowired NotificationRepository notifications;
    @Autowired UserAccountRepository accounts;
    @Autowired PlatformTransactionManager transactionManager;

    @Test void ownerQueriesAndUnreadCountNeverIncludeAnotherOwner() {
        UserAccount owner = accounts.save(new UserAccount("owner", "hash", Role.STUDENT));
        UserAccount other = accounts.save(new UserAccount("other", "hash", Role.LECTURER));
        Notification first = notifications.save(new Notification(owner, NotificationType.REPORT_APPROVED, "First", "Message", "/student/reports"));
        first.markRead();
        notifications.save(new Notification(owner, NotificationType.RESULT_PUBLISHED, "Second", "Message", "/student/grading"));
        notifications.save(new Notification(other, NotificationType.REPORT_SUBMITTED, "Private", "Message", "/lecturer/reports"));
        notifications.flush();

        var page = notifications.findByUserAccountUsername("owner",
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
        assertThat(page).hasSize(2);
        assertThat(notifications.countByUserAccountUsernameAndReadFalse("owner")).isEqualTo(1);
        assertThat(notifications.findByIdAndUserAccountUsername(first.getId(), "other")).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rollbackRemovesNotificationCreatedInsideBusinessTransaction() {
        UserAccount owner = accounts.saveAndFlush(new UserAccount("rollback-owner", "hash", Role.STUDENT));
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            notifications.save(new Notification(owner, NotificationType.REPORT_APPROVED,
                    "Will roll back", "Message", "/student/reports"));
            throw new IllegalStateException("business failure");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(notifications.countByUserAccountUsernameAndReadFalse("rollback-owner")).isZero();
    }
}
