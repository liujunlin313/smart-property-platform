package com.smartproperty.platform.notification.dto;

public class NotificationCreateCommand {
    private Long notificationId;
    private final Long senderAccountId; private final String title; private final String content; private final String category;
    public NotificationCreateCommand(Long senderAccountId, String title, String content, String category) { this.senderAccountId = senderAccountId; this.title = title; this.content = content; this.category = category; }
    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }
    public Long getSenderAccountId() { return senderAccountId; } public String getTitle() { return title; } public String getContent() { return content; } public String getCategory() { return category; }
}
