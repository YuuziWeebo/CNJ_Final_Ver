package com.glucoze.thesismanagement.notification;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserAccountUsername(String username, Pageable pageable);
    long countByUserAccountUsernameAndReadFalse(String username);
    Optional<Notification> findByIdAndUserAccountUsername(Long id, String username);

    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.userAccount.username = :username and n.read = false")
    int markAllReadByUsername(@Param("username") String username);
}
