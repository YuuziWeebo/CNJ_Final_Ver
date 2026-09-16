package com.glucoze.thesismanagement.notification;

import com.glucoze.thesismanagement.common.BaseEntity;
import com.glucoze.thesismanagement.user.entity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_owner_read_created", columnList = "user_account_id,is_read,created_at"),
        @Index(name = "idx_notification_owner_created", columnList = "user_account_id,created_at")
})
public class Notification extends BaseEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_account_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserAccount userAccount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "target_url", nullable = false, length = 300)
    private String targetUrl;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    protected Notification() { }

    public Notification(UserAccount userAccount, NotificationType type, String title, String message, String targetUrl) {
        this.userAccount = userAccount;
        this.type = type;
        this.title = title;
        this.message = message;
        this.targetUrl = targetUrl;
    }

    public UserAccount getUserAccount() { return userAccount; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getTargetUrl() { return targetUrl; }
    public boolean isRead() { return read; }
    public void markRead() { this.read = true; }
}
