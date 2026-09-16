package com.glucoze.thesismanagement.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.glucoze.thesismanagement.common.enums.Role;
import com.glucoze.thesismanagement.config.JpaAuditingConfig;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class BaseEntityAuditingTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void populatesCreatedAndUpdatedTimestampsOnPersistAndUpdate() throws Exception {
        UserAccount account = new UserAccount("audit-user", "password-hash", Role.STUDENT);

        entityManager.persistAndFlush(account);

        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getUpdatedAt()).isNotNull();
        assertThat(account.getUpdatedAt()).isEqualTo(account.getCreatedAt());

        var initialUpdatedAt = account.getUpdatedAt();
        Thread.sleep(20);
        account.setEnabled(false);
        entityManager.flush();

        assertThat(account.getCreatedAt()).isNotNull();
        assertThat(account.getUpdatedAt()).isAfter(initialUpdatedAt);
    }
}
