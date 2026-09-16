package com.glucoze.thesismanagement.notification;

import java.time.LocalDateTime;

public record NotificationView(Long id, NotificationType type, String title, String message,
                               String targetUrl, boolean read, LocalDateTime createdAt) {
}
